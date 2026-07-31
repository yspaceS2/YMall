package com.ymall.backend.order.returnrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnRequestReviewRequest(
    @NotBlank @Size(max = 500) String response
) {
}
