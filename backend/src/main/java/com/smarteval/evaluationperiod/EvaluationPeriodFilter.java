package com.smarteval.evaluationperiod;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class EvaluationPeriodFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String periodIdStr = request.getHeader("X-Evaluation-Period-Id");
            if (periodIdStr != null && !periodIdStr.isEmpty()) {
                try {
                    Long periodId = Long.parseLong(periodIdStr);
                    EvaluationPeriodContext.setCurrentPeriodId(periodId);
                } catch (NumberFormatException e) {
                    // Invalid format, ignore
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            EvaluationPeriodContext.clear();
        }
    }
}
