package com.smarteval.evaluatee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluateeRepository extends JpaRepository<Evaluatee, Long> {
    List<Evaluatee> findByDepartmentIdAndEnabledTrueOrderByIdAsc(Long departmentId);

    long countByDepartmentId(Long departmentId);

    @Query("""
            select e from Evaluatee e
            where (:departmentId is null or e.department.id = :departmentId)
              and (:name = '' or e.name like concat('%', :name, '%'))
              and (:enabled is null or e.enabled = :enabled)
            """)
    Page<Evaluatee> search(
            @Param("departmentId") Long departmentId,
            @Param("name") String name,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );
}
