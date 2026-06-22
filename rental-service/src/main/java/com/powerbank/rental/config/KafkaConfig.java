package com.powerbank.rental.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic acquireCabinetLockEventTopic() {
        return TopicBuilder.name("acquire-cabinet-lock-event").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic acquireCabinetLockResultTopic() {
        return TopicBuilder.name("acquire-cabinet-lock-result").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic ejectPowerBankEventTopic() {
        return TopicBuilder.name("eject-powerbank-event").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic ejectPowerBankResultTopic() {
        return TopicBuilder.name("eject-powerbank-result").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentRequestTopic() {
        return TopicBuilder.name("payment-request").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentResultTopic() {
        return TopicBuilder.name("payment-result").partitions(3).replicas(1).build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
