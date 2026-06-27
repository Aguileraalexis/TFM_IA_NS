package com.tesis.nsdemo.controller;

import com.tesis.nsframework.core.exception.ExternalServiceException;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.ExecutionResult;
import com.tesis.nsframework.core.model.SymbolicState;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ExecutionControllerAdvice {

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ExecutionResult> handleExternalService(ExternalServiceException ex) {
        log.warn("Fallo un servicio externo. servicio={}, status={}, mensaje={}",
                ex.serviceName(), ex.suggestedHttpStatus(), ex.getMessage(), ex);
        return failure(ex.suggestedHttpStatus(), ex.getMessage());
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ExecutionResult> handleRetryable(RetryableException ex) {
        int status = isTimeout(ex) ? 504 : 503;
        String message = status == 504
                ? "Un servicio externo excedio el tiempo de espera al procesar la solicitud"
                : "No se pudo completar la solicitud porque un servicio externo no esta disponible";
        log.warn("Fallo de conectividad con servicio externo. status={}, mensaje={}", status, message, ex);
        return failure(status, message);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ExecutionResult> handleFeign(FeignException ex) {
        int status = mapFeignStatus(ex.status());
        String message = switch (status) {
            case 504 -> "Un servicio externo excedio el tiempo de espera al procesar la solicitud";
            case 503 -> "No se pudo completar la solicitud porque un servicio externo no esta disponible";
            default -> "Un servicio externo devolvio una respuesta invalida o inesperada";
        };
        log.warn("Respuesta de error recibida desde servicio externo. statusOrigen={}, statusApi={}", ex.status(), status, ex);
        return failure(status, message);
    }

    @ExceptionHandler(FrameworkException.class)
    public ResponseEntity<ExecutionResult> handleFramework(FrameworkException ex) {
        log.warn("La solicitud no pudo procesarse: {}", ex.getMessage(), ex);
        return failure(422, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExecutionResult> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return failure(HttpStatus.BAD_REQUEST.value(), message.isBlank() ? "La peticion es invalida" : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExecutionResult> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return failure(HttpStatus.BAD_REQUEST.value(), "El cuerpo de la peticion no tiene un JSON valido");
    }

    private ResponseEntity<ExecutionResult> failure(int status, String message) {
        return ResponseEntity.status(status)
                .body(ExecutionResult.failure(message, List.of(), 0, new SymbolicState()));
    }

    private String formatFieldError(FieldError fieldError) {
        if (fieldError.getDefaultMessage() == null || fieldError.getDefaultMessage().isBlank()) {
            return "El campo " + fieldError.getField() + " es invalido";
        }
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private int mapFeignStatus(int originStatus) {
        if (originStatus == 408 || originStatus == 504) {
            return 504;
        }
        if (originStatus >= 500 || originStatus == -1) {
            return 503;
        }
        return 502;
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        String message = throwable.getMessage();
        return message != null && message.toLowerCase().contains("timed out");
    }
}
