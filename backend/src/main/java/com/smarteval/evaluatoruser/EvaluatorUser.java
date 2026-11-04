package com.smarteval.evaluatoruser;

import com.smarteval.department.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "evaluator_user")
public class EvaluatorUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String username;

    @Column(name = "password_plain", nullable = false, length = 64)
    private String passwordPlain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean submitted;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "evaluation_period_id", nullable = false)
    private Long evaluationPeriodId;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    EvaluatorUser() {
    }

    public EvaluatorUser(String username, String passwordPlain, Department department, boolean enabled) {
        this(username, passwordPlain, department, enabled, false, null);
    }

    public EvaluatorUser(String username, String passwordPlain, Department department,
                         boolean enabled, boolean submitted, OffsetDateTime submittedAt) {
        this.username = username;
        this.passwordPlain = passwordPlain;
        this.department = department;
        this.enabled = enabled;
        this.submitted = submitted;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordPlain() {
        return passwordPlain;
    }

    public Department getDepartment() {
        return department;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updatePasswordPlain(String passwordPlain) {
        this.passwordPlain = passwordPlain;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void markSubmitted(OffsetDateTime submittedAt) {
        this.submitted = true;
        this.submittedAt = submittedAt;
    }

    public Long getEvaluationPeriodId() {
        return evaluationPeriodId;
    }

    public void setEvaluationPeriodId(Long evaluationPeriodId) {
        this.evaluationPeriodId = evaluationPeriodId;
    }
}
