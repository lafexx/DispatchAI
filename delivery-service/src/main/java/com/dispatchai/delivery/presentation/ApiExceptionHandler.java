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
  public ResponseEntity<ApiError> handleNotFound(DeliveryNotFoundException ex) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new ApiError(ex.getMessage()));
  }

  @ExceptionHandler(InvalidDeliveryStateException.class)
  public ResponseEntity<ApiError> handleConflict(InvalidDeliveryStateException ex) {

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new ApiError(ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(ex.getMessage()));
  }
}
