package com.dispatchai.dispatch.presentation.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dispatchai.dispatch.domain.exception.AssignmentNotFoundException;
import com.dispatchai.dispatch.domain.exception.DeliveryNotAssignableException;
import com.dispatchai.dispatch.domain.exception.DeliveryNotFoundException;
import com.dispatchai.dispatch.domain.exception.InvalidAssignmentStateException;
import com.dispatchai.dispatch.domain.exception.NoAvailableDriverException;

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

  @ExceptionHandler(com.dispatchai.dispatch.domain.exception.ReservationConflictException.class)
  public ResponseEntity<ApiError> reservationConflict(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(HttpStatus.CONFLICT, ex.getMessage()));
  }

  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
    var message = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage()).collect(java.util.stream.Collectors.joining("; "));
    return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST, message));
  }
  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> unreadable(Exception ex) {
    return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST, "Invalid request body"));
  }

  @ExceptionHandler(org.springframework.web.client.RestClientResponseException.class)
  public ResponseEntity<ApiError> dependency(org.springframework.web.client.RestClientResponseException ex) {
    var status = ex.getStatusCode().value() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
    return ResponseEntity.status(status).body(ApiError.of(status, "Delivery or fleet dependency could not supply the requested resource"));
  }
  @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
  public ResponseEntity<ApiError> transport(Exception ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError.of(HttpStatus.SERVICE_UNAVAILABLE, "Delivery or fleet dependency timed out or could not be reached"));
  }

  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ResponseEntity<ApiError> status(org.springframework.web.server.ResponseStatusException ex) {
    var status = HttpStatus.valueOf(ex.getStatusCode().value());
    return ResponseEntity.status(status).body(ApiError.of(status,ex.getReason() == null ? status.getReasonPhrase() : ex.getReason()));
  }
  @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> argument(Exception ex) { return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST,"Invalid request parameter")); }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"));
  }
}
