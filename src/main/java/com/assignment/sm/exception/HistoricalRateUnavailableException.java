package com.assignment.sm.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class HistoricalRateUnavailableException extends RuntimeException{
  private static final long serialVersionUID = -14819139347930534L;
  private HttpStatus httpStatus;
  private Map<String, String> body;

  public HistoricalRateUnavailableException(HttpStatus httpStatus, String message){
    super(message);
    this.body = new HashMap<>();
    this.body.put("msg", message);
    this.httpStatus = httpStatus;
  }

  public ResponseEntity getResponse(){
    return ResponseEntity.status(this.httpStatus).body(this.body);
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String toString() {
    return getResponse() + " " + getMessage();
  }
}
