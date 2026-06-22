package com.powerbank.user.dto;

import lombok.Data;

@Data
public class VerifyRequest {
    private String phone;
    private String otp;
}
