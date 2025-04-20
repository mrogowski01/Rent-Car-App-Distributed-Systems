package pl.edu.agh.car_service.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pl.edu.agh.car_service.Models.Response.ResponseMessage;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseMessage> handleGenericException(Exception ex) {
        return ResponseEntity.badRequest().body(new ResponseMessage(400, "An error occurred"));
    }
}
