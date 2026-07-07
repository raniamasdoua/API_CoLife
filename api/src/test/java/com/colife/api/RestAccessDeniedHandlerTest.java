package com.colife.api;

import com.colife.api.shared.security.RestAccessDeniedHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestAccessDeniedHandlerTest {

    @Test
    void handle_sets_403_status_and_json_content_type() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(mockMapper);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.handle(req, res, new AccessDeniedException("denied"));

        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentType()).contains("application/json");
        assertThat(res.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        verify(mockMapper).writeValue(any(java.io.Writer.class), any());
    }

    @Test
    void handle_does_nothing_when_response_is_already_committed() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(mockMapper);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setCommitted(true);

        handler.handle(req, res, new AccessDeniedException("denied"));

        assertThat(res.getStatus()).isEqualTo(200);
        verify(mockMapper, never()).writeValue(any(java.io.Writer.class), any());
    }
}
