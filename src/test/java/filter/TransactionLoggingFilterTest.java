package filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.epam.gym.filter.TransactionLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TransactionLoggingFilterTest {

    private final TransactionLoggingFilter filter = new TransactionLoggingFilter();

    private Logger logbackLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUpLogCapture() {
        logbackLogger = (Logger) LoggerFactory.getLogger(TransactionLoggingFilter.class);
        logbackLogger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    void shouldSkipWrapping_whenRequestIsNotHttp() throws Exception {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldWrapRequestAndResponse_andPassThemToChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<ServletRequest> capturedRequest = new AtomicReference<>();
        AtomicReference<ServletResponse> capturedResponse = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            capturedRequest.set(req);
            capturedResponse.set(res);
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedRequest.get()).isInstanceOf(ContentCachingRequestWrapper.class);
        assertThat(capturedResponse.get()).isInstanceOf(ContentCachingResponseWrapper.class);
    }

    @Test
    void shouldPutTransactionIdInMdc_duringChain_andClearAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> transactionIdDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> transactionIdDuringChain.set(MDC.get("transactionId"));

        filter.doFilter(request, response, chain);

        assertThat(transactionIdDuringChain.get()).isNotBlank();
        assertThat(MDC.get("transactionId")).isNull();
    }

    @Test
    void shouldClearMdc_evenWhenChainThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));

        assertThat(MDC.get("transactionId")).isNull();
    }

    @Test
    void shouldCopyResponseBodyBack_toRealResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainings");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String body = "{\"result\":\"ok\"}";

        FilterChain chain = (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write(body);
            ((HttpServletResponse) res).setStatus(201);
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getContentAsString()).isEqualTo(body);
        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    void shouldLogErrorLevel_whenResponseStatusIsClientError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainings/999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(404);

        filter.doFilter(request, response, chain);

        boolean hasErrorLog = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.ERROR
                        && e.getFormattedMessage().contains("status=404"));

        assertThat(hasErrorLog).isTrue();
    }

    @Test
    void shouldLogInfoLevel_whenResponseIsSuccessful() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(200);

        filter.doFilter(request, response, chain);

        boolean hasSuccessLog = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.INFO
                        && e.getFormattedMessage().contains("status=200"));

        assertThat(hasSuccessLog).isTrue();
    }

    @Test
    void shouldLogRequestPayload_whenBodyIsPresentAndRead() throws Exception {
        String requestJson = "{\"firstName\":\"John\"}";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainees");
        request.setContent(requestJson.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // ContentCachingRequestWrapper caches bytes only as they are actually read
        FilterChain chain = (req, res) -> req.getInputStream().readAllBytes();

        filter.doFilter(request, response, chain);

        boolean hasPayloadLog = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.DEBUG
                        && e.getFormattedMessage().contains("John"));

        assertThat(hasPayloadLog).isTrue();
    }

    @Test
    void shouldNotLogRequestPayload_whenBodyIsEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainees");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        boolean hasPayloadLog = appender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().startsWith("Request Payload:"));

        assertThat(hasPayloadLog).isFalse();
    }

    @Test
    void shouldTruncatePayload_whenLongerThan5000Bytes() throws Exception {
        String longBody = "a".repeat(6000);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainees");
        request.setContent(longBody.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> req.getInputStream().readAllBytes();

        filter.doFilter(request, response, chain);

        String prefix = "Request Payload: ";
        Optional<String> payloadMessage = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith(prefix))
                .findFirst();

        assertThat(payloadMessage).isPresent();
        assertThat(payloadMessage.get().substring(prefix.length())).hasSize(5000);
    }

    @Test
    void shouldReplaceNewlinesInPayload_withSpaces() throws Exception {
        String bodyWithNewlines = "{\n\"firstName\":\"John\"\r\n}";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainees");
        request.setContent(bodyWithNewlines.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> req.getInputStream().readAllBytes();

        filter.doFilter(request, response, chain);

        boolean hasCleanPayload = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith("Request Payload:"))
                .anyMatch(m -> !m.contains("\n") && !m.contains("\r"));

        assertThat(hasCleanPayload).isTrue();
    }
}