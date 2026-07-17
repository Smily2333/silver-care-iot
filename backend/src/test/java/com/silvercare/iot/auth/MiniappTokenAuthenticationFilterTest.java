package com.silvercare.iot.auth;

import com.silvercare.iot.security.MiniappPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiniappTokenAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerTokenCreatesAuthenticatedPrincipal() throws Exception {
        MiniappAuthService authService = mock(MiniappAuthService.class);
        when(authService.authenticate("valid-token")).thenReturn(Optional.of(new MiniappPrincipal(10L)));
        MiniappTokenAuthenticationFilter filter = new MiniappTokenAuthenticationFilter(authService);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/miniapp/devices/DEV001/overview");
        request.addHeader("Authorization", "Bearer valid-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(new MiniappPrincipal(10L));
    }
}
