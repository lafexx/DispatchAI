package com.dispatchai.agent.presentation.error;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import com.dispatchai.agent.domain.exception.NoAvailableDriverException;
import com.dispatchai.agent.domain.exception.InvalidDispatchRecommendationException;
@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(InvalidDispatchRecommendationException.class)
  public ResponseEntity<ApiError> invalid(RuntimeException e) { return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiError.of(HttpStatus.BAD_GATEWAY,e.getMessage())); }
  @ExceptionHandler(NoAvailableDriverException.class)
  public ResponseEntity<ApiError> unavailable(RuntimeException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(HttpStatus.CONFLICT,e.getMessage())); }
  @ExceptionHandler(RestClientResponseException.class)
  public ResponseEntity<ApiError> downstream(RestClientResponseException e) {
    int status=e.getStatusCode().value();
    if (status==404 || status==409) return ResponseEntity.status(status).body(ApiError.of(HttpStatus.valueOf(status),"Delivery or resource is unavailable; refresh and retry"));
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError.of(HttpStatus.SERVICE_UNAVAILABLE,"A dispatch dependency is unavailable"));
  }
  @ExceptionHandler(ResourceAccessException.class)
  public ResponseEntity<ApiError> transport(ResourceAccessException e) { return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError.of(HttpStatus.SERVICE_UNAVAILABLE,"A dispatch dependency timed out or could not be reached")); }
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
