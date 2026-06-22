package com.powerbank.station.grpc;

import com.powerbank.proto.station.*;
import com.powerbank.station.dto.StationResponse;
import com.powerbank.station.kafka.StationEventProducer;
import com.powerbank.station.service.StationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class StationGrpcService extends StationServiceGrpc.StationServiceImplBase {

    private final StationService stationService;
    private final StationEventProducer eventProducer;

    @Override
    public void getStation(GetStationRequest request, StreamObserver<GetStationResponse> responseObserver) {
        try {
            StationResponse station = stationService.getStation(UUID.fromString(request.getStationId()));
            responseObserver.onNext(toProto(station));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getStation failed", e);
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getNearbyStations(GetNearbyStationsRequest request,
                                  StreamObserver<GetNearbyStationsResponse> responseObserver) {
        try {
            List<StationResponse> stations = stationService.getNearbyStations(
                    request.getLatitude(), request.getLongitude(), request.getRadiusKm());

            GetNearbyStationsResponse.Builder builder = GetNearbyStationsResponse.newBuilder();
            stations.forEach(s -> builder.addStations(toProto(s)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getNearbyStations failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void lockSlot(LockSlotRequest request, StreamObserver<LockSlotResponse> responseObserver) {
        try {
            String slotId = stationService.lockSlot(UUID.fromString(request.getStationId()));
            responseObserver.onNext(LockSlotResponse.newBuilder()
                    .setSuccess(true)
                    .setSlotId(slotId)
                    .setMessage("Slot locked successfully")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("lockSlot failed for station {}", request.getStationId(), e);
            responseObserver.onNext(LockSlotResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void ejectPowerBank(EjectPowerBankRequest request,
                               StreamObserver<EjectPowerBankResponse> responseObserver) {
        try {
            // Fire-and-forget: publish event, return immediately
            eventProducer.publishEjectEvent(
                    request.getRentalId(), request.getStationId(), request.getSlotId());

            responseObserver.onNext(EjectPowerBankResponse.newBuilder()
                    .setSuccess(true)
                    .setPowerBankId(request.getSlotId())
                    .setMessage("Eject request queued")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("ejectPowerBank failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void returnPowerBank(ReturnPowerBankRequest request,
                                StreamObserver<ReturnPowerBankResponse> responseObserver) {
        try {
            String slotId = stationService.returnPowerBank(
                    UUID.fromString(request.getPowerBankId()),
                    UUID.fromString(request.getStationId()));

            responseObserver.onNext(ReturnPowerBankResponse.newBuilder()
                    .setSuccess(true)
                    .setSlotId(slotId)
                    .setMessage("PowerBank returned successfully")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("returnPowerBank failed", e);
            responseObserver.onNext(ReturnPowerBankResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private GetStationResponse toProto(StationResponse s) {
        return GetStationResponse.newBuilder()
                .setStationId(s.getId())
                .setName(s.getName())
                .setLatitude(s.getLatitude())
                .setLongitude(s.getLongitude())
                .setAvailableSlots(s.getAvailableSlots())
                .setStatus(s.getStatus())
                .build();
    }
}
