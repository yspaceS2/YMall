package com.ymall.backend.dashboard.service;

import java.time.LocalDate;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

enum DashboardPeriod {
    SEVEN_DAYS("7d", Interval.DAY),
    THIRTY_DAYS("30d", Interval.DAY),
    SIX_MONTHS("6m", Interval.MONTH),
    ONE_YEAR("1y", Interval.MONTH);

    private final String code;
    private final Interval interval;

    DashboardPeriod(String code, Interval interval) {
        this.code = code;
        this.interval = interval;
    }

    static DashboardPeriod from(String value) {
        for (DashboardPeriod period : values()) {
            if (period.code.equalsIgnoreCase(value)) {
                return period;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    String code() {
        return code;
    }

    Interval interval() {
        return interval;
    }

    LocalDate from(LocalDate today) {
        return switch (this) {
            case SEVEN_DAYS -> today.minusDays(6);
            case THIRTY_DAYS -> today.minusDays(29);
            case SIX_MONTHS -> today.withDayOfMonth(1).minusMonths(5);
            case ONE_YEAR -> today.withDayOfMonth(1).minusMonths(11);
        };
    }

    enum Interval {
        DAY,
        MONTH
    }
}
