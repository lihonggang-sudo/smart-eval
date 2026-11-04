package com.smarteval.evaluationperiod;

import com.smarteval.common.BusinessException;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluatee.EvaluateeRepository;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EvaluationPeriodService {
    private final EvaluationPeriodRepository repository;
    private final DepartmentRepository departmentRepository;
    private final EvaluateeRepository evaluateeRepository;
    private final EvaluatorUserRepository evaluatorUserRepository;
    private final EvaluationRecordRepository evaluationRecordRepository;

    public EvaluationPeriodService(
            EvaluationPeriodRepository repository,
            DepartmentRepository departmentRepository,
            EvaluateeRepository evaluateeRepository,
            EvaluatorUserRepository evaluatorUserRepository,
            EvaluationRecordRepository evaluationRecordRepository
    ) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
        this.evaluateeRepository = evaluateeRepository;
        this.evaluatorUserRepository = evaluatorUserRepository;
        this.evaluationRecordRepository = evaluationRecordRepository;
    }

    @Transactional
    public EvaluationPeriod create(String name) {
        EvaluationPeriod period = new EvaluationPeriod(name);
        return repository.save(period);
    }

    @Transactional(readOnly = true)
    public List<EvaluationPeriod> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<EvaluationPeriod> getLatest() {
        return repository.findFirstByOrderByCreatedAtDesc();
    }

    @Transactional
    public void delete(Long id) {
        long totalCount = repository.count();
        if (totalCount <= 1) {
            throw new BusinessException("不能删除最后一个批次");
        }

        boolean hasData = departmentRepository.existsByEvaluationPeriodId(id)
                || evaluateeRepository.existsByEvaluationPeriodId(id)
                || evaluatorUserRepository.existsByEvaluationPeriodId(id)
                || evaluationRecordRepository.existsByEvaluationPeriodId(id);

        if (hasData) {
            throw new BusinessException("该批次下存在数据，无法删除。请先清空批次数据后再删除。");
        }

        repository.deleteById(id);
    }
}
