package com.dispatchai.fleet.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dispatchai.fleet.domain.model.exceptions.DriverNotFoundException;
import com.dispatchai.fleet.domain.model.exceptions.VehicleNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

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
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"));
  }
}
