package com.smarteval.evaluationperiod;

public class EvaluationPeriodContext {
    private static final ThreadLocal<Long> currentPeriodId = new ThreadLocal<>();

    public static void setCurrentPeriodId(Long periodId) {
        currentPeriodId.set(periodId);
    }

    public static Long getCurrentPeriodId() {
        return currentPeriodId.get();
    }

    public static void clear() {
        currentPeriodId.remove();
    }
}
