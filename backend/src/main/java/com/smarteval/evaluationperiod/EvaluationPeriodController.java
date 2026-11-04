package com.smarteval.evaluationperiod;

import com.smarteval.common.ApiResponse;
import com.smarteval.evaluationperiod.dto.CreateEvaluationPeriodRequest;
import com.smarteval.evaluationperiod.dto.EvaluationPeriodListItem;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/evaluation-periods")
public class EvaluationPeriodController {
    private final EvaluationPeriodService service;

    public EvaluationPeriodController(EvaluationPeriodService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<EvaluationPeriodListItem>> list() {
        List<EvaluationPeriodListItem> items = service.findAll().stream()
                .map(p -> new EvaluationPeriodListItem(p.getId(), p.getName(), p.getCreatedAt().toString()))
                .toList();
        return ApiResponse.ok(items);
    }

    @PostMapping
    public ApiResponse<EvaluationPeriodListItem> create(@Valid @RequestBody CreateEvaluationPeriodRequest request) {
        EvaluationPeriod period = service.create(request.name());
        return ApiResponse.ok(new EvaluationPeriodListItem(
                period.getId(), period.getName(), period.getCreatedAt().toString()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
