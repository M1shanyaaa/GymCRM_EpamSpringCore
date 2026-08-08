package com.epam.gym.client;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FeignClientInterceptorTest {

    private final FeignClientInterceptor interceptor = new FeignClientInterceptor();

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void apply_copiesAuthorizationHeaderFromCurrentRequest() {
        // given: incoming request with Authorization header
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();

        // when
        interceptor.apply(template);

        // then
        assertThat(template.headers().get("Authorization"))
                .containsExactly("Bearer test-token");
    }

    @Test
    void apply_copiesTransactionIdFromMdc() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("transactionId", "tx-123");

        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("X-Transaction-Id"))
                .containsExactly("tx-123");
    }

    @Test
    void apply_copiesBothHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("transactionId", "tx-999");

        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer abc");
        assertThat(template.headers().get("X-Transaction-Id")).containsExactly("tx-999");
    }

    @Test
    void apply_noRequestContext_doesNotAddAuthHeader() {
        // no RequestContextHolder set
        RequestContextHolder.resetRequestAttributes();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void apply_noAuthHeaderInRequest_doesNotAddIt() {
        MockHttpServletRequest request = new MockHttpServletRequest(); // без Authorization
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).isNull();
    }

    @Test
    void apply_noTransactionIdInMdc_doesNotAddHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.clear(); // немає transactionId

        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("X-Transaction-Id")).isNull();
    }
}