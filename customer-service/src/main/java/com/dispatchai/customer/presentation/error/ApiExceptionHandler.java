package com.dispatchai.customer.presentation.error;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(com.dispatchai.customer.domain.exception.CustomerNotFoundException.class)
  public ResponseEntity<ApiError> missing(RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(HttpStatus.NOT_FOUND,e.getMessage())); }
  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
    var message = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()+": "+e.getDefaultMessage())
        .collect(java.util.stream.Collectors.joining("; "));
    return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST,message));
  }
  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> unreadable(Exception ex) { return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST,"Invalid request body")); }
  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ResponseEntity<ApiError> status(org.springframework.web.server.ResponseStatusException ex) {
    var status = HttpStatus.valueOf(ex.getStatusCode().value());
    return ResponseEntity.status(status).body(ApiError.of(status,ex.getReason() == null ? status.getReasonPhrase() : ex.getReason()));
  }
  @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> argument(Exception ex) { return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST,"Invalid request parameter")); }
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("Unhandled API exception",e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occurred"));
  }
}
