package com.mouradeveloper.digitalwallet.exception;

import com.mouradeveloper.digitalwallet.exception.business.BusinessRuleException;
import com.mouradeveloper.digitalwallet.exception.notFound.NotFoundException;
import com.mouradeveloper.digitalwallet.models.dto.error.StandardError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<StandardError> handleNotFoundException(
            NotFoundException ex,
            HttpServletRequest request
    ){
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<StandardError> handleBusinessRuleException(
            BusinessRuleException ex,
            HttpServletRequest request
    ){
        return buildErrorResponse(ex, HttpStatus.UNPROCESSABLE_CONTENT, request.getRequestURI());
    }


    private static ResponseEntity<StandardError> buildErrorResponse(
            Exception ex,
            HttpStatus status,
            String path
    ){
        var standardError = StandardError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(path)
                .build();

        return  new ResponseEntity<>(standardError, status);
    }
}
