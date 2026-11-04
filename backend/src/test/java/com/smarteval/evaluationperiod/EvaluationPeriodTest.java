package com.smarteval.evaluationperiod;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationPeriodTest {

    @Test
    void shouldCreateEvaluationPeriod() {
        EvaluationPeriod period = new EvaluationPeriod("2026年11月测评");
        assertThat(period.getName()).isEqualTo("2026年11月测评");
    }

    @Test
    void shouldUpdateName() {
        EvaluationPeriod period = new EvaluationPeriod("原名称");
        period.update("新名称");
        assertThat(period.getName()).isEqualTo("新名称");
    }
}
