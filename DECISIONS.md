# DECISIONS.md

## 1. Architecture Decisions

### UUID vs BIGSERIAL
UUID tanladik chunki:
- Security: BIGSERIAL ketma-ket (1,2,3) bo'lgani uchun 
  hacker keyingi ID ni taxmin qila oladi. UUID random 
  bo'lgani uchun taxmin qilib bo'lmaydi.
- Microservices: Har bir servis o'z ID sini mustaqil 
  generate qiladi, conflict bo'lmaydi.
- Keycloak user ID ham UUID formatida keladi.
- Kamchilik: UUID indeks performance da BIGSERIAL ga 
  qaraganda biroz sekin (random tartib), lekin security 
  muhimroq deb qaror qildik.

### NUMERIC vs Double
Barcha pul maydonlarida NUMERIC(19,2) ishlatdik chunki:
- Double da floating point xatosi bor: 
  0.1 + 0.2 = 0.30000000000000004
- Moliyaviy operatsiyalarda aniq natija kerak.
- NUMERIC da: 0.1 + 0.2 = 0.3 (aniq)
- Bank tizimlarida Double ishlatish jiddiy xato.

### Indekslar
- idx_users_phone: Har authentication da phone bilan 
  qidiramiz, indekssiz full table scan bo'ladi.
- idx_stations_lat_lng: Yaqin stansiyalarni koordinata 
  bilan qidiramiz.
- idx_stations_status: Faqat ACTIVE stansiyalar kerak.
- idx_powerbanks_station_status: "Shu stansiyada AVAILABLE 
  powerbank bormi?" tez topish uchun.
- idx_rentals_idempotency_key: Duplicate request tekshirish 
  uchun, UNIQUE constraint ham qo'shildi.
- idx_rentals_user_created: User tarixi sahifasi uchun 
  (user_id + created_at DESC).

### Hibernate DDL
hibernate.ddl-auto=validate ishlatdik.
Barcha migratsiyalar Liquibase orqali boshqariladi.
Hibernate faqat entity va DB strukturasi mosligini 
tekshiradi. hibernate.ddl-auto=create ishlatilmadi.

### TIMESTAMPTZ vs TIMESTAMP
TIMESTAMPTZ ishlatdik chunki:
- TIMESTAMP timezone saqlamaydi.
- TIMESTAMPTZ timezone bilan saqlaydi (UTC).
- Foydalanuvchi Toshkentda ijara boshlasa, server 
  Londonda bo'lsa ham vaqt to'g'ri ko'rsatiladi.

### gRPC vs REST (servislar orasida)
Servislar orasida gRPC ishlatdik chunki:
- gRPC binary (Proto) format ishlatadi, JSON ga qaraganda 
  5-10x tez.
- JSON serialize/deserialize ko'p vaqt oladi.
- Type-safe: Proto file kontrakt vazifasini o'taydi, 
  kompilyator xatolarni topadi.
- Payment service da gRPC yo'q chunki payment ga 
  to'g'ridan-to'g'ri hech kim call qilmaydi, 
  faqat Kafka orqali ishlaydi.

### Kong API Gateway
Kong 3.7 DB-less mode da API Gateway sifatida ishlatildi.

Implement qilingan:
- Declarative routing (kong.yml)
- user-service: /auth/phone, /auth/verify, /v1/auth/refresh
- rental-service: /api/v1/rentals/**, /api/v1/rentals/finish
- JWT plugin: Keycloak RS256 public key bilan token verify
- Public endpoints (/auth/phone, /auth/verify) → token siz
- Protected endpoints (/api/v1/rentals/**) → JWT required

JWT verification:
- Keycloak RS256 public key ishlatildi
- Token expired bo'lsa → 401
- Token yo'q bo'lsa → 401
- Token valid → request servicega uzatiladi

### Keycloak
Keycloak OAuth2 server sifatida ishlatildi:
- JWT access token va refresh token yaratish.
- Realm: powerbank-realm, Client: powerbank-app (public).
- User /auth/verify da Keycloak da avtomatik yaratiladi.
- user-service Keycloak Admin API orqali user yaratadi,
  keyin password grant bilan JWT oladi.
- MVP uchun development mode (start-dev).

Production da qilinishi kerak:
- realm-export.json bilan docker-compose auto-import.
- Confidential client + Kong OIDC introspection.
- start mode + HTTPS.

### Idempotency Key
Duplicate requestlarni oldini olish uchun:
- Foydalanuvchi internet muammosi sababli bir necha marta 
  bosishi mumkin.
- Har request uchun client tomondan unique key yaratiladi.
- Xuddi shu key kelsa → mavjud natijani qaytaramiz (200).
- Yangi request → yangi key → yangi resurs (201).
- Agar xuddi shu key, boshqa amount kelsa → birinchi 
  requestni qaytaramiz, warning log ga yoziladi.

### FSM (Finite State Machine)
Rental lifecycle uchun FSM ishlatdik:
- Har qadam nazorat qilinadi.
- LOCKING_STATION bo'lmasa PROCESSING_PAYMENT yo'q.
- PROCESSING_PAYMENT bo'lmasa EJECTING_POWERBANK yo'q.
- Istalgan joyda xato bo'lsa → FAILED state.
- Tartibsiz o'tishlar oldini olinadi.

---

## 2. Kafka Design

### Kafka vs gRPC
- gRPC: Darhol javob kerak bo'lganda (User mavjudmi? 
  Station bormi?)
- Kafka: Javob keyinroq kelsa bo'lganda (Pul yech, 
  Powerbank chiqar)
- Kafka asinxron: rental-service xabar yuboradi va 
  kutmaydi, o'z ishini davom ettiradi.

### Kafka key
rentalId va stationId ni key sifatida ishlatdik chunki:
- Bir xil key → bir Partition ga boradi.
- Partition ichida tartib saqlanadi.
- rental-001 barcha eventlari bir Partition da ketma-ket: 
  Lock → Payment → Eject ✅
- Key bo'lmasa: Lock Partition 2, Payment Partition 0 
  → tartib buziladi ❌

### Kafka tushib qolsa
Hozirgi holat:
- Kafka o'chiq → exception → log ga yoziladi
- Xabar yo'qoladi ❌

Yechim (Outbox Pattern, vaqt bo'lganda):
- DB ga rental saqlanganda outbox table ga ham yoziladi 
  (bir transaction ichida, atomik).
- Background job outbox dan o'qib Kafka ga yuboradi.
- Kafka ishlab ketsa → outbox dan xabar Kafka ga uzatiladi.
- Yuborildi → outbox dan o'chiriladi.

### DB va Kafka transaction
Muammo:
- DB ga rental saqlandi ✅
- Kafka ga event yuborilmadi ❌ (Kafka o'chiq)
- Rental abadiy WAITING da qoladi.

Yechim: Outbox Pattern (yuqorida aytilgan).
MVP uchun vaqt yetmagani sababli implement qilinmadi.

### OTP Delivery
Real implementatsiyada Telegram Bot API ishlatiladi yoki SMS message.
MVP uchun OTP server log da chiqariladi (development mode).
OTP response body da qaytarilmaydi — security best practice.

---

## 3. What I Would Do With More Time

- Outbox Pattern: DB va Kafka orasida atomik transaction.
- PostGIS: getNearbyStations() da ST_DWithin() bilan 
  haqiqiy geo-radius filtering.
- Telegram Bot API: OTP ni Telegram orqali yuborish.
- Kong gRPC Transcoding: To'liq sozlash.
- Recurring payments: Ijara davomida har soat to'lov.
- Unit va Integration testlar yozish.
- Rate limiting: Kong da har user uchun limit.
- Circuit breaker: Servislar orasida Resilience4j.
- Docker Compose da barcha servislar birgalikda run.

---

## 4. Questions

- Outbox Pattern ni Spring da qanday implement qilish 
  eng yaxshi usul? (Polling vs CDC - Debezium)
- Kong gRPC Transcoding sozlashda proto file ni 
  qanday to'g'ri register qilish kerak?
- Keycloak realm export qilib docker-compose da 
  avtomatik import qilish mumkinmi?
- PostGIS ni Spring Data JPA bilan ishlatishda 
  qaysi library yaxshiroq: Hibernate Spatial yoki 
  jooq?
- Kafka consumer group rebalancing vaqtida 
  message processing to'xtab qolmasligini 
  qanday ta'minlash mumkin?
