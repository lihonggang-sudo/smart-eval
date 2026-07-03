package com.smarteval.evaluation;

import com.smarteval.auth.AuthService;
import com.smarteval.common.BusinessException;
import com.smarteval.evaluatee.Evaluatee;
import com.smarteval.evaluatee.EvaluateeRepository;
import com.smarteval.evaluation.dto.EvaluateeItem;
import com.smarteval.evaluation.dto.EvaluationItemResponse;
import com.smarteval.evaluation.dto.GradeItem;
import com.smarteval.evaluation.dto.SubmitEvaluationRecord;
import com.smarteval.evaluation.dto.SubmitEvaluationRequest;
import com.smarteval.evaluation.dto.SubmitEvaluationResponse;
import com.smarteval.evaluatoruser.EvaluatorUser;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
import com.smarteval.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EvaluationService {
    private static final String MISSING_EVALUATIONS_MESSAGE = "必须完成本部门全部人员评价后才能提交";
    private static final String INVALID_EVALUATEE_MESSAGE = "只能评价本部门启用人员";
    private static final String ALREADY_SUBMITTED_MESSAGE = "您已提交评价";

    private final AuthService authService;
    private final EvaluatorUserRepository evaluatorUserRepository;
    private final EvaluateeRepository evaluateeRepository;
    private final EvaluationRecordRepository evaluationRecordRepository;

    public EvaluationService(
            AuthService authService,
            EvaluatorUserRepository evaluatorUserRepository,
            EvaluateeRepository evaluateeRepository,
            EvaluationRecordRepository evaluationRecordRepository
    ) {
        this.authService = authService;
        this.evaluatorUserRepository = evaluatorUserRepository;
        this.evaluateeRepository = evaluateeRepository;
        this.evaluationRecordRepository = evaluationRecordRepository;
    }

    @Transactional(readOnly = true)
    public EvaluationItemResponse getItems(CurrentUser currentUser) {
        EvaluatorUser evaluatorUser = authService.requireEnabledEvaluator(currentUser);
        List<Evaluatee> evaluatees = evaluateeRepository.findByDepartmentIdAndEnabledTrueOrderByIdAsc(
                evaluatorUser.getDepartment().getId());

        return new EvaluationItemResponse(
                evaluatorUser.getDepartment().getId(),
                evaluatorUser.getDepartment().getName(),
                evaluatorUser.isSubmitted(),
                evaluatees.stream()
                        .map(evaluatee -> new EvaluateeItem(evaluatee.getId(), evaluatee.getName()))
                        .toList(),
                Arrays.stream(EvaluationGrade.values())
                        .map(grade -> new GradeItem(grade.name(), grade.label()))
                        .toList()
        );
    }

    @Transactional
    public SubmitEvaluationResponse submit(CurrentUser currentUser, SubmitEvaluationRequest request) {
        authService.requireEnabledEvaluator(currentUser);
        EvaluatorUser evaluatorUser = evaluatorUserRepository.findByIdForUpdate(currentUser.id())
                .filter(EvaluatorUser::isEnabled)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (evaluatorUser.isSubmitted()) {
            throw new BusinessException(ALREADY_SUBMITTED_MESSAGE);
        }

        List<Evaluatee> expectedEvaluatees = evaluateeRepository.findByDepartmentIdAndEnabledTrueOrderByIdAsc(
                evaluatorUser.getDepartment().getId());
        List<SubmitEvaluationRecord> submittedRecords = request.records() == null ? List.of() : request.records();
        validateSubmittedRecords(submittedRecords);
        Set<Long> expectedIds = expectedEvaluatees.stream()
                .map(Evaluatee::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> submittedIds = submittedRecords.stream()
                .map(SubmitEvaluationRecord::evaluateeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (submittedRecords.size() != submittedIds.size() || !expectedIds.containsAll(submittedIds)) {
            throw new BusinessException(INVALID_EVALUATEE_MESSAGE);
        }
        if (submittedRecords.isEmpty() || !submittedIds.containsAll(expectedIds)) {
            throw new BusinessException(MISSING_EVALUATIONS_MESSAGE);
        }

        Map<Long, Evaluatee> evaluateeById = expectedEvaluatees.stream()
                .collect(Collectors.toMap(Evaluatee::getId, Function.identity()));
        List<EvaluationRecord> records = submittedRecords.stream()
                .map(record -> toEvaluationRecord(evaluatorUser, evaluateeById, record))
                .toList();
        evaluationRecordRepository.saveAll(records);

        OffsetDateTime submittedAt = OffsetDateTime.now();
        evaluatorUser.markSubmitted(submittedAt);
        return new SubmitEvaluationResponse(true, submittedAt.toInstant());
    }

    private EvaluationRecord toEvaluationRecord(
            EvaluatorUser evaluatorUser,
            Map<Long, Evaluatee> evaluateeById,
            SubmitEvaluationRecord record
    ) {
        Evaluatee evaluatee = evaluateeById.get(record.evaluateeId());
        if (evaluatee == null) {
            throw new BusinessException(INVALID_EVALUATEE_MESSAGE);
        }

        return new EvaluationRecord(
                evaluatorUser,
                evaluatee,
                evaluatorUser.getDepartment(),
                record.grade(),
                record.comment1(),
                record.comment2()
        );
    }

    private void validateSubmittedRecords(List<SubmitEvaluationRecord> submittedRecords) {
        for (SubmitEvaluationRecord record : submittedRecords) {
            if (record == null || record.evaluateeId() == null || record.grade() == null) {
                throw new BusinessException(INVALID_EVALUATEE_MESSAGE);
            }
        }
    }
}
