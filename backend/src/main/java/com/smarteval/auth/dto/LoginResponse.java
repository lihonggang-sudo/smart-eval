package com.smarteval.auth.dto;

import com.smarteval.evaluationperiod.dto.LatestPeriodInfo;

public record LoginResponse(String token, Object user, LatestPeriodInfo latestPeriod) {
}
