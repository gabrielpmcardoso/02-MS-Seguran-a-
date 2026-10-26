package br.com.exemplo.login;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(DuplicateKeyException.class)
 ResponseEntity<?> duplicate() { return ResponseEntity.status(409).body(Map.of("mensagem","Username já cadastrado")); }
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,IllegalArgumentException.class})
 ResponseEntity<?> invalid() { return ResponseEntity.badRequest().body(Map.of("mensagem","JSON ou campos inválidos; senha deve ter no máximo 72 bytes UTF-8")); }
}
