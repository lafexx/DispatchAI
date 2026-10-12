package com.dispatchai.fleet.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dispatchai.fleet.domain.model.exceptions.DriverNotFoundException;
import com.dispatchai.fleet.domain.model.exceptions.VehicleNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(DriverNotFoundException.class)
  public ResponseEntity<ApiError> handleDriverNotFound(
      DriverNotFoundException ex) {

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(
            HttpStatus.NOT_FOUND,
            ex.getMessage()));
  }

  @ExceptionHandler(VehicleNotFoundException.class)
  public ResponseEntity<ApiError> handleVehicleNotFound(
      VehicleNotFoundException ex) {

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiError.of(
            HttpStatus.NOT_FOUND,
            ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpectedException(
      Exception ex) {

    log.error("Unhandled exception", ex);

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"));
  }
}
