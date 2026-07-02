package com.smartcbwtf.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.smartcbwtf.config.JwtService;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.FeatureGuardService;
import com.smartcbwtf.service.SubscriptionService;
import com.smartcbwtf.service.SystemConfigService;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for deprecated InvoiceController.
 * 
 * Verifies that all invoice endpoints return 410 Gone status
 * and include appropriate deprecation message.
 */
@WebMvcTest(InvoiceController.class)
class InvoiceControllerDeprecationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeatureGuardService featureGuardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private SystemConfigService systemConfigService;

    @Test
    @DisplayName("POST /api/invoices/generate should return 410 Gone")
    @WithMockUser(roles = "CBWTF_ADMIN")
    void generateShouldReturn410() throws Exception {
        mockMvc.perform(post("/api/invoices/generate")
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value(containsString("Tally")))
                .andExpect(jsonPath("$.replacement").value("/api/cbwtf/billing/bills"));
    }

    @Test
    @DisplayName("GET /api/invoices/list should return 410 Gone")
    @WithMockUser(roles = "CBWTF_ADMIN")
    void listShouldReturn410() throws Exception {
        mockMvc.perform(get("/api/invoices/list"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value(containsString("Tally")));
    }

    @Test
    @DisplayName("GET /api/invoices/{id}/pdf should return 410 Gone")
    @WithMockUser(roles = "CBWTF_ADMIN")
    void pdfShouldReturn410() throws Exception {
        mockMvc.perform(get("/api/invoices/123e4567-e89b-12d3-a456-426614174000/pdf"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value(containsString("Tally")));
    }

    @Test
    @DisplayName("GET /api/invoices should return 410 Gone")
    @WithMockUser(roles = "CBWTF_ADMIN")
    void getAllShouldReturn410() throws Exception {
        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value(containsString("Tally")));
    }

    @Test
    @DisplayName("GET /api/invoices/{id} should return 410 Gone")
    @WithMockUser(roles = "CBWTF_ADMIN")
    void getByIdShouldReturn410() throws Exception {
        mockMvc.perform(get("/api/invoices/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value(containsString("Tally")));
    }

    @Test
    @DisplayName("Deprecated invoice controller should stay CBWTF-admin scoped")
    void invoiceControllerShouldStayCbwtfAdminScoped() {
        PreAuthorize annotation = InvoiceController.class.getAnnotation(PreAuthorize.class);

        assertEquals("hasRole('CBWTF_ADMIN')", annotation.value());
    }
}
