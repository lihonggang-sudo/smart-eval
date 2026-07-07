package com.smarteval.web;

import com.smarteval.common.GlobalExceptionHandler;
import com.smarteval.security.ApiAccessDeniedHandler;
import com.smarteval.security.ApiAuthenticationEntryPoint;
import com.smarteval.security.SecurityConfig;
import com.smarteval.security.TokenAuthenticationFilter;
import com.smarteval.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpaForwardController.class)
@Import({
        SecurityConfig.class,
        TokenAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class SpaForwardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void forwardsAdminLoginRouteToFrontendIndexWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void forwardsMobileLoginRouteToFrontendIndexWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/mobile/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
