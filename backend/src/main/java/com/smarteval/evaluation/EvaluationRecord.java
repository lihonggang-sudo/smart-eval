package com.smarteval.evaluation;

import com.smarteval.department.Department;
import com.smarteval.evaluatee.Evaluatee;
import com.smarteval.evaluatoruser.EvaluatorUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "evaluation_record")
public class EvaluationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_user_id", nullable = false)
    private EvaluatorUser evaluatorUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluatee_id", nullable = false)
    private Evaluatee evaluatee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvaluationGrade grade;

    @Column(length = 255)
    private String comment1;

    @Column(length = 255)
    private String comment2;

    @Column(name = "evaluation_period_id", nullable = false)
    private Long evaluationPeriodId;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    EvaluationRecord() {
    }

    public EvaluationRecord(EvaluatorUser evaluatorUser, Evaluatee evaluatee, Department department,
                            EvaluationGrade grade, String comment1, String comment2) {
        this.evaluatorUser = evaluatorUser;
        this.evaluatee = evaluatee;
        this.department = department;
        this.grade = grade;
        this.comment1 = comment1;
        this.comment2 = comment2;
    }

    public Long getId() {
        return id;
    }

    public EvaluatorUser getEvaluatorUser() {
        return evaluatorUser;
    }

    public Evaluatee getEvaluatee() {
        return evaluatee;
    }

    public Department getDepartment() {
        return department;
    }

    public EvaluationGrade getGrade() {
        return grade;
    }

    public String getComment1() {
        return comment1;
    }

    public String getComment2() {
        return comment2;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getEvaluationPeriodId() {
        return evaluationPeriodId;
    }

    public void setEvaluationPeriodId(Long evaluationPeriodId) {
        this.evaluationPeriodId = evaluationPeriodId;
    }
}
