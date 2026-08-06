package com.epam.gym.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class TransactionLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TransactionLoggingFilter.class);
    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final int MAX_PAYLOAD_LENGTH = 5000;
    private static final char UTF8_REPLACEMENT_CHAR = '\uFFFD';

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest req) || !(response instanceof HttpServletResponse res)) {
            chain.doFilter(request, response);
            return;
        }

        String uri = req.getRequestURI();
        if (uri.startsWith("/actuator") || uri.startsWith("/swagger") || uri.startsWith("/v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        // 1. Generate transactionId and add it to MDC
        String transactionId = UUID.randomUUID().toString();
        MDC.put(TRANSACTION_ID_KEY, transactionId);

        // 2. Wrap request/response to allow multiple reads of the body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(req);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(res);

        long startTime = System.currentTimeMillis();

        try {
            String safeUri = maskSensitiveUri(req.getRequestURI(), req.getQueryString());
            log.info("--> Incoming Request: method={} uri={}", req.getMethod(), safeUri);
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

            // Copy the response body back so the client receives it
            wrappedResponse.copyBodyToResponse();

            // Clear MDC to prevent memory leaks in thread pools
            MDC.clear();
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
        String regex = "(?i)(\"([^\"]*(password|secret|token|key|jwt)[^\"]*)\"\\s*:\\s*\")([^\"]+)(\")";
        return payload.replaceAll(regex, "$1****$4");
    }

    private String maskSensitiveUri(String requestURI, String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return requestURI;
        }
        String maskedQuery = queryString.replaceAll("(?i)(token|secret|password|key|jwt)=([^&]+)", "$1=****");
        return requestURI + "?" + maskedQuery;
    }
}