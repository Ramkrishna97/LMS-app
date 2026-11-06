package com.example.lms.config; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestControllerAdvice public class ApiErrors {
 @ExceptionHandler({IllegalArgumentException.class,java.util.NoSuchElementException.class}) ResponseEntity<Map<String,String>> error(RuntimeException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) ResponseEntity<Map<String,String>> tooLarge(){return ResponseEntity.status(413).body(Map.of("message","The upload exceeds the 250 MB source-file limit."));}
}
