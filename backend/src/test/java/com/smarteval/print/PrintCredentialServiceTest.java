package com.smarteval.print;

import com.smarteval.evaluatoruser.dto.EvaluatorUserListItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrintCredentialServiceTest {
    private final PrintCredentialService printCredentialService = new PrintCredentialService("http://localhost:5173/mobile/login");

    @Test
    void rendersHtmlWithMobileUrlAndCredentialFields() {
        EvaluatorUserListItem item = new EvaluatorUserListItem(
                1L,
                "D001-0001",
                "A8K2P9",
                1L,
                "组织部",
                true,
                false,
                null,
                Instant.parse("2026-07-03T00:00:00Z")
        );

        String html = printCredentialService.renderHtml(List.of(item));

        assertThat(html).contains("http://localhost:5173/mobile/login");
        assertThat(html).contains("组织部");
        assertThat(html).contains("D001-0001");
        assertThat(html).contains("A8K2P9");
        assertThat(html).contains("@media print");
    }
}
