package com.smarteval.evaluation;

import com.smarteval.result.dto.ResultSummaryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationRecordRepository extends JpaRepository<EvaluationRecord, Long> {
    boolean existsByEvaluatorUserId(Long evaluatorUserId);

    boolean existsByEvaluateeId(Long evaluateeId);

    long countByDepartmentId(Long departmentId);

    @Query(value = """
            select new com.smarteval.result.dto.ResultSummaryItem(
                ee.id,
                ee.name,
                d.name,
                sum(case when r.grade = com.smarteval.evaluation.EvaluationGrade.EXCELLENT then 1 else 0 end),
                sum(case when r.grade = com.smarteval.evaluation.EvaluationGrade.COMPETENT then 1 else 0 end),
                sum(case when r.grade = com.smarteval.evaluation.EvaluationGrade.BASIC_COMPETENT then 1 else 0 end),
                sum(case when r.grade = com.smarteval.evaluation.EvaluationGrade.INCOMPETENT then 1 else 0 end),
                count(r)
            )
            from EvaluationRecord r
            join r.evaluatee ee
            join r.department d
            where (:departmentId is null or d.id = :departmentId)
              and (:evaluateeName = '' or ee.name like concat('%', :evaluateeName, '%'))
            group by ee.id, ee.name, d.id, d.name
            order by ee.id asc
            """,
            countQuery = """
                    select count(distinct concat(concat(str(ee.id), ':'), str(d.id)))
                    from EvaluationRecord r
                    join r.evaluatee ee
                    join r.department d
                    where (:departmentId is null or d.id = :departmentId)
                      and (:evaluateeName = '' or ee.name like concat('%', :evaluateeName, '%'))
                    """)
    Page<ResultSummaryItem> findResultSummaries(
            @Param("departmentId") Long departmentId,
            @Param("evaluateeName") String evaluateeName,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"evaluatorUser", "evaluatorUser.department", "evaluatee"})
    @Query(value = """
            select r
            from EvaluationRecord r
            join r.evaluatorUser eu
            join r.evaluatee ee
            join r.department d
            where (:departmentId is null or d.id = :departmentId)
              and (:evaluateeId is null or ee.id = :evaluateeId)
              and (:evaluatorUserId is null or eu.id = :evaluatorUserId)
              and (:grade is null or r.grade = :grade)
            order by r.id asc
            """,
            countQuery = """
                    select count(r)
                    from EvaluationRecord r
                    join r.evaluatorUser eu
                    join r.evaluatee ee
                    join r.department d
                    where (:departmentId is null or d.id = :departmentId)
                      and (:evaluateeId is null or ee.id = :evaluateeId)
                      and (:evaluatorUserId is null or eu.id = :evaluatorUserId)
                      and (:grade is null or r.grade = :grade)
                    """)
    Page<EvaluationRecord> findResultDetails(
            @Param("departmentId") Long departmentId,
            @Param("evaluateeId") Long evaluateeId,
            @Param("evaluatorUserId") Long evaluatorUserId,
            @Param("grade") EvaluationGrade grade,
            Pageable pageable
    );
}
