package com.assignment.sm.controller.exceptionHandler;

import com.assignment.sm.exception.CurrencyNotFoundException;
import com.assignment.sm.exception.ExchangeRateFetchException;
import com.assignment.sm.exception.ExchangeRateParseException;
import com.assignment.sm.exception.ExchangeRateSaveException;
import com.assignment.sm.exception.InvalidInputException;
import com.assignment.sm.model.ErrorDetails;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ExchangeRateFetchException.class)
  public ResponseEntity processFetchException(ExchangeRateFetchException e, WebRequest request){
    ErrorDetails errorDetails = ErrorDetails.builder()
                                                .localDateTime(LocalDateTime.now())
                                                .message(e.getLocalizedMessage())
                                                .details(request.getDescription(false))
                                                .build();
    return new ResponseEntity(errorDetails,e. getHttpStatus());
  }

  @ExceptionHandler(ExchangeRateSaveException.class)
  public ResponseEntity processSaveException(ExchangeRateSaveException e, WebRequest request){
    ErrorDetails errorDetails = ErrorDetails.builder()
                                                .localDateTime(LocalDateTime.now())
                                                .message(e.getLocalizedMessage())
                                                .details(request.getDescription(false))
                                                .build();
    return new ResponseEntity(errorDetails,e. getHttpStatus());
  }

  @ExceptionHandler(ExchangeRateParseException.class)
  public ResponseEntity processParseException(ExchangeRateParseException e, WebRequest request){
    ErrorDetails errorDetails = ErrorDetails.builder()
                                              .localDateTime(LocalDateTime.now())
                                              .message(e.getLocalizedMessage())
                                              .details(request.getDescription(false))
                                              .build();
    return new ResponseEntity(errorDetails,e. getHttpStatus());
  }

  @ExceptionHandler(CurrencyNotFoundException.class)
  public ResponseEntity processNotFoundException(CurrencyNotFoundException e, WebRequest request){
    ErrorDetails errorDetails = ErrorDetails.builder()
                                              .localDateTime(LocalDateTime.now())
                                              .message(e.getLocalizedMessage())
                                              .details(request.getDescription(false))
                                              .build();
    return new ResponseEntity(errorDetails,e. getHttpStatus());
  }

  @ExceptionHandler(InvalidInputException.class)
  public ResponseEntity processInvalidInputException(InvalidInputException e, WebRequest request){
    ErrorDetails errorDetails = ErrorDetails.builder()
                                            .localDateTime(LocalDateTime.now())
                                            .message(e.getLocalizedMessage())
                                            .details(request.getDescription(false))
                                            .build();
    return new ResponseEntity(errorDetails,e. getHttpStatus());
  }
}
