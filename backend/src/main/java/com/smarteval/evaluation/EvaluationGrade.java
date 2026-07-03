package com.smarteval.evaluation;

public enum EvaluationGrade {
    EXCELLENT("优秀"),
    COMPETENT("称职"),
    BASIC_COMPETENT("基本称职"),
    INCOMPETENT("不称职");

    private final String label;

    EvaluationGrade(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
