package com.ymall.backend.dashboard.dto;

import java.time.LocalDate;

public record DashboardPeriodResponse(
    String period,
    LocalDate from,
    LocalDate to,
    String interval
) {
}
