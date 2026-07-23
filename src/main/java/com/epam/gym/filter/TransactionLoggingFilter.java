package com.epam.gym.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TransactionLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TransactionLoggingFilter.class);
    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final int MAX_PAYLOAD_LENGTH = 5000;
    private static final char UTF8_REPLACEMENT_CHAR = '\uFFFD';

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 1. Generate transactionId and add it to MDC
        String transactionId = UUID.randomUUID().toString();
        MDC.put(TRANSACTION_ID_KEY, transactionId);

        // 2. Wrap request/response to allow multiple reads of the body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(req);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(res);

        long startTime = System.currentTimeMillis();

        try {
            log.info("--> Incoming Request: method={} uri={}", req.getMethod(), req.getRequestURI());

            // Pass the request further along the chain
            chain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = wrappedResponse.getStatus();

            // Read the request body safely
            String requestBody = getPayload(wrappedRequest.getContentAsByteArray());
            if (!requestBody.isBlank()) {
                log.debug("Request Payload: {}", requestBody);
            }

            // Read the response body safely
            String responseBody = getPayload(wrappedResponse.getContentAsByteArray());

            // Log the result (200 OK or errors)
            if (status >= 400) {
                log.error("<-- Outgoing Error Response: status={} duration={}ms body={}", status, duration, responseBody);
            } else {
                log.info("<-- Outgoing Success Response: status={} duration={}ms", status, duration);
                if (!responseBody.isBlank()) {
                    log.debug("Response Payload: {}", responseBody);
                }
            }

            // IMPORTANT: Copy the response body back so the client receives it
            wrappedResponse.copyBodyToResponse();

            // Clear MDC to prevent memory leaks in thread pools
            MDC.remove(TRANSACTION_ID_KEY);
        }
    }

    private String getPayload(byte[] buf) {
        if (buf == null || buf.length == 0) return "";
        int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
        String truncated = new String(buf, 0, length, StandardCharsets.UTF_8);
        if (!truncated.isEmpty() && truncated.charAt(truncated.length() - 1) == UTF8_REPLACEMENT_CHAR) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }

        String payload = truncated.replaceAll("[\\r\\n]+", " ");
        return maskSensitiveData(payload);
    }

    private String maskSensitiveData(String payload) {
        String regex = "(?i)(\"([^\"]*password[^\"]*)\"\\s*:\\s*\")([^\"]+)(\")";
        return payload.replaceAll(regex, "$1****$4");
    }
}