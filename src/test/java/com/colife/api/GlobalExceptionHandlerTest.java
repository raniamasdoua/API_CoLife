package com.colife.api;

import com.colife.api.shared.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");

    @Test
    void handleNotFound_returns_404_with_message() {
        ResponseEntity<ApiError> res = handler.handleNotFound(
                new ResourceNotFoundException("Ressource introuvable"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().message()).isEqualTo("Ressource introuvable");
        assertThat(res.getBody().status()).isEqualTo(404);
        assertThat(res.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleBusinessException_returns_400_with_message() {
        ResponseEntity<ApiError> res = handler.handleBusinessException(
                new BusinessException("Règle métier non respectée"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).isEqualTo("Règle métier non respectée");
        assertThat(res.getBody().status()).isEqualTo(400);
    }

    @Test
    void handleForbidden_returns_403_with_message() {
        ResponseEntity<ApiError> res = handler.handleForbidden(
                new ForbiddenException("Accès interdit"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().message()).isEqualTo("Accès interdit");
        assertThat(res.getBody().status()).isEqualTo(403);
    }

    @Test
    void handleConflict_returns_409_with_message() {
        ResponseEntity<ApiError> res = handler.handleConflict(
                new ConflictException("Conflit détecté"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().message()).isEqualTo("Conflit détecté");
        assertThat(res.getBody().status()).isEqualTo(409);
    }

    @Test
    void handleBadCredentials_returns_401_with_fixed_message() {
        ResponseEntity<ApiError> res = handler.handleBadCredentials(
                new BadCredentialsException("bad pwd"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().message()).isEqualTo("Credentials invalides");
    }

    @Test
    void handleUnauthorized_returns_401_with_message() {
        ResponseEntity<ApiError> res = handler.handleUnauthorized(
                new UnauthorizedException("Token expiré"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().message()).isEqualTo("Token expiré");
    }

    @Test
    void handleAuthentication_returns_401_with_fixed_message() {
        AuthenticationException ex = mock(AuthenticationException.class);
        ResponseEntity<ApiError> res = handler.handleAuthentication(ex, req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().message()).isEqualTo("Authentification requise");
    }

    @Test
    void handleAccessDenied_returns_403_with_fixed_message() {
        ResponseEntity<ApiError> res = handler.handleAccessDenied(
                new AccessDeniedException("not allowed"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().message()).isEqualTo("Accès refusé");
    }

    @Test
    void handleValidation_returns_400_with_first_field_error() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "ne doit pas être vide");
        when(br.getFieldErrors()).thenReturn(List.of(fieldError));
        when(ex.getBindingResult()).thenReturn(br);

        ResponseEntity<ApiError> res = handler.handleValidation(ex, req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("email");
    }

    @Test
    void handleValidation_returns_default_message_when_no_field_errors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(List.of());
        when(ex.getBindingResult()).thenReturn(br);

        ResponseEntity<ApiError> res = handler.handleValidation(ex, req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).isEqualTo("Erreur de validation");
    }

    @Test
    void handleConstraint_returns_400_with_message() {
        ResponseEntity<ApiError> res = handler.handleConstraint(
                new ConstraintViolationException("contrainte", Set.of()), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).isEqualTo("contrainte");
    }

    @Test
    void handleTypeMismatch_returns_400_with_param_name() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");

        ResponseEntity<ApiError> res = handler.handleTypeMismatch(ex, req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).contains("id");
    }

    @Test
    void handleIllegalArgument_returns_400_with_message() {
        ResponseEntity<ApiError> res = handler.handleIllegalArgument(
                new IllegalArgumentException("Argument invalide"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).isEqualTo("Argument invalide");
    }

    @Test
    void handleGeneric_returns_500_with_fixed_message() {
        ResponseEntity<ApiError> res = handler.handleGeneric(
                new RuntimeException("crash inattendu"), req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().message()).isEqualTo("Erreur interne du serveur");
        assertThat(res.getBody().status()).isEqualTo(500);
    }
}
