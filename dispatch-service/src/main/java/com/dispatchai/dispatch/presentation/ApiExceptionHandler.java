package com.dispatchai.dispatch.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dispatchai.dispatch.domain.model.exceptions.AssignmentNotFoundException;
import com.dispatchai.dispatch.domain.model.exceptions.DeliveryNotAssignableException;
import com.dispatchai.dispatch.domain.model.exceptions.DeliveryNotFoundException;
import com.dispatchai.dispatch.domain.model.exceptions.InvalidAssignmentStateException;
import com.dispatchai.dispatch.domain.model.exceptions.NoAvailableDriverException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(AssignmentNotFoundException.class)
  public ResponseEntity<ApiError> handleAssignmentNotFound(
      AssignmentNotFoundException ex) {

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(
            HttpStatus.NOT_FOUND,
            ex.getMessage()));
  }

  @ExceptionHandler(DeliveryNotFoundException.class)
  public ResponseEntity<ApiError> handleDeliveryNotFound(
      DeliveryNotFoundException ex) {

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(
            HttpStatus.NOT_FOUND,
            ex.getMessage()));
  }

  @ExceptionHandler(DeliveryNotAssignableException.class)
  public ResponseEntity<ApiError> handleDeliveryNotAssignable(
      DeliveryNotAssignableException ex) {

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ApiError.of(
            HttpStatus.CONFLICT,
            ex.getMessage()));
  }

  @ExceptionHandler(InvalidAssignmentStateException.class)
  public ResponseEntity<ApiError> handleInvalidAssignmentState(
      InvalidAssignmentStateException ex) {

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ApiError.of(
            HttpStatus.CONFLICT,
            ex.getMessage()));
  }

  @ExceptionHandler(NoAvailableDriverException.class)
  public ResponseEntity<ApiError> handleNoAvailableDriver(
      NoAvailableDriverException ex) {

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
