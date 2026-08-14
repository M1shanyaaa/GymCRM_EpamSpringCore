package com.epam.gym.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        // 1. Extract JWT token from the current incoming request and pass it downstream
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null) {
                template.header(AUTHORIZATION_HEADER, authHeader);
            }
        }

        // 2. Extract Transaction ID from the logging context (MDC) and add it to the header
        String transactionId = MDC.get(TRANSACTION_ID_KEY);
        if (transactionId != null) {
            template.header(TRANSACTION_ID_HEADER, transactionId);
        }
    }
}