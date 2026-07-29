package security;

import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.security.AuthenticationInterceptor;
import com.epam.gym.security.NoAuth;
import com.epam.gym.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    private static final String AUTH_USER_HEADER = "X-Auth-Username";
    private static final String AUTH_PASS_HEADER = "X-Auth-Password";

    @Mock private AuthService authService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HandlerMethod handlerMethod;

    private AuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthenticationInterceptor(authService);
    }

    @Test
    void preHandle_shouldSkip_whenHandlerIsNotHandlerMethod() {
        Object handler = new Object();

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verifyNoInteractions(authService);
    }

    @Test
    void preHandle_shouldSkip_whenNoAuthAnnotationPresent() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
        verifyNoInteractions(authService);
        verify(request, never()).getHeader(anyString());
    }

    @Test
    void preHandle_shouldUsePathVariableUsername_whenPresent() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(false);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("username", "John.Smith"));
        when(request.getHeader(AUTH_PASS_HEADER)).thenReturn("raw");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
        verify(authService).authenticate("John.Smith", "raw");
        // path variable takes priority — header username must never be consulted
        verify(request, never()).getHeader(AUTH_USER_HEADER);
    }

    @Test
    void preHandle_shouldFallBackToHeaderUsername_whenNoPathVariable() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(false);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(null);
        when(request.getHeader(AUTH_USER_HEADER)).thenReturn("John.Smith");
        when(request.getHeader(AUTH_PASS_HEADER)).thenReturn("raw");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void preHandle_shouldFallBackToHeaderUsername_whenPathVariableMapHasNoUsernameKey() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(false);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("someOtherVar", "value"));
        when(request.getHeader(AUTH_USER_HEADER)).thenReturn("John.Smith");
        when(request.getHeader(AUTH_PASS_HEADER)).thenReturn("raw");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void preHandle_shouldThrow_whenUsernameMissing() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(false);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(null);
        when(request.getHeader(AUTH_USER_HEADER)).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining(AUTH_USER_HEADER);

        verifyNoInteractions(authService);
    }

    @Test
    void preHandle_shouldThrow_whenUsernameBlank() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(false);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("username", "  "));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining(AUTH_USER_HEADER);

        verifyNoInteractions(authService);
    }

    @Test
    void preHandle_shouldThrow_whenPasswordMissing() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(false);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(null);
        when(request.getHeader(AUTH_USER_HEADER)).thenReturn("John.Smith");
        when(request.getHeader(AUTH_PASS_HEADER)).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining(AUTH_PASS_HEADER);

        verifyNoInteractions(authService);
    }

    @Test
    void preHandle_shouldThrow_whenPasswordBlank() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(false);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(null);
        when(request.getHeader(AUTH_USER_HEADER)).thenReturn("John.Smith");
        when(request.getHeader(AUTH_PASS_HEADER)).thenReturn(" ");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining(AUTH_PASS_HEADER);

        verifyNoInteractions(authService);
    }

    @Test
    void preHandle_shouldPropagate_whenAuthServiceRejectsCredentials() {
        when(handlerMethod.hasMethodAnnotation(NoAuth.class)).thenReturn(false);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(null);
        when(request.getHeader(AUTH_USER_HEADER)).thenReturn("John.Smith");
        when(request.getHeader(AUTH_PASS_HEADER)).thenReturn("wrong");
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(authService).authenticate("John.Smith", "wrong");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid username or password");
    }
}