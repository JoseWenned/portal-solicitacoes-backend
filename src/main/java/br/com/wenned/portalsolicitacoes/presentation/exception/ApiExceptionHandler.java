package br.com.wenned.portalsolicitacoes.presentation.exception;

import br.com.wenned.portalsolicitacoes.domain.exception.autenticacao.AutenticacaoInvalidaException;
import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.OperacaoSolicitacaoInvalidaException;
import br.com.wenned.portalsolicitacoes.domain.exception.solicitacoes.SolicitacaoNaoEncontradaException;
import br.com.wenned.portalsolicitacoes.domain.exception.usuarios.EmailJaCadastradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<FieldError> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldError(
                error.getField(),
                error.getDefaultMessage()
            ))
            .toList();

        return response(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Existem campos inválidos.",
            request,
            errors
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidData(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            exception.getMessage(),
            request,
            List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> invalidParameter(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "Um parâmetro da requisição possui formato inválido.",
            request,
            List.of(new FieldError(
                exception.getName(),
                "Formato inválido."
            ))
        );
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ApiError> duplicateEmail(
        EmailJaCadastradoException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.CONFLICT,
            "EMAIL_ALREADY_REGISTERED",
            exception.getMessage(),
            request,
            List.of()
        );
    }

    @ExceptionHandler(SolicitacaoNaoEncontradaException.class)
    public ResponseEntity<ApiError> requestNotFound(
        SolicitacaoNaoEncontradaException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.NOT_FOUND,
            "REQUEST_NOT_FOUND",
            exception.getMessage(),
            request,
            List.of()
        );
    }

    @ExceptionHandler(OperacaoSolicitacaoInvalidaException.class)
    public ResponseEntity<ApiError> requestConflict(
        OperacaoSolicitacaoInvalidaException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.CONFLICT,
            "REQUEST_STATE_CONFLICT",
            exception.getMessage(),
            request,
            List.of()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> invalidJson(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST_BODY",
            "O corpo da requisição está ausente ou é inválido.",
            request,
            List.of()
        );
    }

    @ExceptionHandler(AutenticacaoInvalidaException.class)
    public ResponseEntity<ApiError> authentication(
        AutenticacaoInvalidaException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.UNAUTHORIZED,
            "AUTHENTICATION_FAILED",
            exception.getMessage(),
            request,
            List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        LOGGER.error(
            "Erro inesperado em {}: {}",
            request.getRequestURI(),
            exception.getClass().getName()
        );

        return response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "Não foi possível concluir a operação.",
            request,
            List.of()
        );
    }

    private ResponseEntity<ApiError> response(
        HttpStatus status,
        String code,
        String message,
        HttpServletRequest request,
        List<FieldError> errors
    ) {
        return ResponseEntity.status(status).body(new ApiError(
            status.value(),
            code,
            message,
            request.getRequestURI(),
            Instant.now(),
            errors
        ));
    }

    public record ApiError(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
    ) {
    }

    public record FieldError(String field, String message) {
    }
}