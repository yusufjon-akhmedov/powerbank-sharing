package com.powerbank.rental.grpc;

import com.powerbank.proto.rental.*;
import com.powerbank.rental.entity.Rental;
import com.powerbank.rental.service.RentalService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RentalGrpcService extends RentalServiceGrpc.RentalServiceImplBase {

    private final RentalService rentalService;

    @Override
    public void createRental(CreateRentalRequest request,
                             StreamObserver<CreateRentalResponse> responseObserver) {
        try {
            Rental rental = rentalService.createOrReturnExisting(
                    request.getUserId(), request.getStationId(),
                    request.getCardId(), request.getIdempotencyKey());

            if ("WAITING".equals(rental.getStatus())) {
                rentalService.initiateRental(rental);
            }

            responseObserver.onNext(CreateRentalResponse.newBuilder()
                    .setRentalId(rental.getId().toString())
                    .setStatus(rental.getStatus())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("createRental gRPC failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getRentalStatus(GetRentalStatusRequest request,
                                StreamObserver<GetRentalStatusResponse> responseObserver) {
        try {
            Rental rental = rentalService.getById(request.getRentalId());
            responseObserver.onNext(toStatusResponse(rental));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getRentalStatus gRPC failed", e);
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getRentalHistory(GetRentalHistoryRequest request,
                                 StreamObserver<GetRentalHistoryResponse> responseObserver) {
        try {
            int page = request.getPage() > 0 ? request.getPage() : 0;
            int size = request.getSize() > 0 ? request.getSize() : 20;

            List<Rental> rentals = rentalService.getHistory(request.getUserId(), page, size);
            GetRentalHistoryResponse.Builder builder = GetRentalHistoryResponse.newBuilder();
            rentals.forEach(r -> builder.addRentals(toStatusResponse(r)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getRentalHistory gRPC failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void finishRental(FinishRentalRequest request,
                             StreamObserver<FinishRentalResponse> responseObserver) {
        try {
            var response = rentalService.finishRental(
                    request.getRentalId(), request.getUserId(), request.getStationId());

            responseObserver.onNext(FinishRentalResponse.newBuilder()
                    .setSuccess(response.isSuccess())
                    .setMessage(response.getMessage() != null ? response.getMessage() : "")
                    .setTotalAmount(response.getTotalAmount() != null
                            ? response.getTotalAmount().doubleValue() : 0.0)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("finishRental gRPC failed", e);
            responseObserver.onNext(FinishRentalResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private GetRentalStatusResponse toStatusResponse(Rental rental) {
        GetRentalStatusResponse.Builder b = GetRentalStatusResponse.newBuilder()
                .setRentalId(rental.getId().toString())
                .setStatus(rental.getStatus());
        if (rental.getPowerBankId() != null) b.setPowerBankId(rental.getPowerBankId());
        if (rental.getSlotId() != null)      b.setSlotId(rental.getSlotId());
        if (rental.getStartedAt() != null)   b.setStartedAt(rental.getStartedAt().toString());
        return b.build();
    }
}
