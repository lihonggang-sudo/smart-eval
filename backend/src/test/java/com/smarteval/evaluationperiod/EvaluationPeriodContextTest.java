package com.smarteval.evaluationperiod;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationPeriodContextTest {

    @Test
    void shouldStoreAndRetrievePeriodId() {
        EvaluationPeriodContext.setCurrentPeriodId(123L);
        assertThat(EvaluationPeriodContext.getCurrentPeriodId()).isEqualTo(123L);
        EvaluationPeriodContext.clear();
        assertThat(EvaluationPeriodContext.getCurrentPeriodId()).isNull();
    }
}
