package com.smarteval.evaluatoruser;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluatorUserRepository extends JpaRepository<EvaluatorUser, Long> {
    Optional<EvaluatorUser> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EvaluatorUser e where e.id = :id")
    Optional<EvaluatorUser> findByIdForUpdate(@Param("id") Long id);

    boolean existsByUsername(String username);

    boolean existsByEvaluationPeriodId(Long evaluationPeriodId);

    long countByDepartmentId(Long departmentId);

    long countBySubmittedTrue();

    @Query("""
            select e from EvaluatorUser e
            where (:departmentId is null or e.department.id = :departmentId)
              and (:submitted is null or e.submitted = :submitted)
              and (:enabled is null or e.enabled = :enabled)
            """)
    Page<EvaluatorUser> search(
            @Param("departmentId") Long departmentId,
            @Param("submitted") Boolean submitted,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );

    @Query("""
            select e from EvaluatorUser e
            where (:departmentId is null or e.department.id = :departmentId)
              and (:submitted is null or e.submitted = :submitted)
              and (:enabled is null or e.enabled = :enabled)
            order by e.id asc
            """)
    List<EvaluatorUser> findForPrint(
            @Param("departmentId") Long departmentId,
            @Param("submitted") Boolean submitted,
            @Param("enabled") Boolean enabled
    );
}
