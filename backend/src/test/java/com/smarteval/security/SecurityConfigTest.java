package com.smarteval.security;

import com.smarteval.common.GlobalExceptionHandler;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestAdminController.class)
@Import({
        SecurityConfig.class,
        TokenAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void returnsJsonEnvelopeForUnauthorizedAdminRequest() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    void returnsJsonEnvelopeForForbiddenAdminRequest() throws Exception {
        when(jwtService.parse("evaluator-token"))
                .thenReturn(new CurrentUser(9L, UserRole.EVALUATOR, "eva"));

        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer evaluator-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.errors").isMap());
    }

    @RestController
    static class TestAdminController {
        @GetMapping("/api/admin/test")
        String adminOnly() {
            return "ok";
        }
    }
}
