package com.dispatchai.delivery.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dispatchai.delivery.domain.model.exceptions.DeliveryNotFoundException;
import com.dispatchai.delivery.domain.model.exceptions.InvalidDeliveryStateException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(DeliveryNotFoundException.class)
  public ResponseEntity<ApiError> handleDeliveryNotFound(
      DeliveryNotFoundException ex) {

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(
            HttpStatus.NOT_FOUND,
            ex.getMessage()));
  }

  @ExceptionHandler(InvalidDeliveryStateException.class)
  public ResponseEntity<ApiError> handleInvalidDeliveryState(
      InvalidDeliveryStateException ex) {

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ApiError.of(
            HttpStatus.CONFLICT,
            ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"));
  }
}
