package com.example.notifications.exception;

import com.example.notifications.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.merge(error.getField(), error.getDefaultMessage(), this::mergeMessages));
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.merge(fieldName(violation), violation.getMessage(), this::mergeMessages);
        }
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(ex.getName(), "Invalid value: " + ex.getValue());
        return error(HttpStatus.BAD_REQUEST, "Invalid request parameter", request, fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String field = result.getMethodParameter().getParameterName();
            field = field == null ? "parameter" : field;
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                fieldErrors.merge(field, error.getDefaultMessage(), this::mergeMessages);
            }
        }
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(ex.getParameterName(), "Required request parameter is missing");
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        InvalidFormatException invalidFormat = findInvalidFormatException(ex);
        if (invalidFormat != null && !invalidFormat.getPath().isEmpty()) {
            String field = invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getFieldName();
            fieldErrors.put(field, "Invalid value: " + invalidFormat.getValue());
        }
        return error(HttpStatus.BAD_REQUEST, "Malformed request body", request, fieldErrors);
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotificationNotFound(
            NotificationNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicateRequestConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateRequestConflict(
            DuplicateRequestConflictException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return error(status, ex.getReason(), request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }

    private String mergeMessages(String first, String second) {
        return first + "; " + second;
    }

    private InvalidFormatException findInvalidFormatException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException) {
                return invalidFormatException;
            }
            current = current.getCause();
        }
        return null;
    }
}
