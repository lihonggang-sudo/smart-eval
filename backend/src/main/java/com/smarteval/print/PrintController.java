package com.smarteval.print;

import com.smarteval.evaluatoruser.EvaluatorUserService;
import com.smarteval.security.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrintController {
    private static final MediaType HTML_UTF8 = MediaType.parseMediaType("text/html;charset=UTF-8");

    private final EvaluatorUserService evaluatorUserService;
    private final PrintCredentialService printCredentialService;

    public PrintController(EvaluatorUserService evaluatorUserService, PrintCredentialService printCredentialService) {
        this.evaluatorUserService = evaluatorUserService;
        this.printCredentialService = printCredentialService;
    }

    @GetMapping("/api/admin/evaluator-users/print")
    public ResponseEntity<String> print(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean submitted,
            @RequestParam(required = false) Boolean enabled
    ) {
        String html = printCredentialService.renderHtml(
                evaluatorUserService.listForPrint(currentUser, departmentId, submitted, enabled)
        );
        return ResponseEntity.ok()
                .contentType(HTML_UTF8)
                .body(html);
    }
}
