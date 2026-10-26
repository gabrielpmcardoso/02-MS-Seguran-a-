package br.com.exemplo.login;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
@RestController
public class UserController {
 private final UserService service;
 public UserController(UserService service) { this.service=service; }
 public record Credentials(@NotBlank @Size(max=100) @Pattern(regexp="[a-zA-Z0-9_.-]+") String username,@NotBlank @Size(max=72) String senha) {}
 public record PasswordChange(@NotBlank @Size(max=72) String atual,@NotBlank @Size(max=72) String nova) {}
 private ResponseEntity<Map<String,String>> response(int status,String message) { return ResponseEntity.status(status).body(Map.of("mensagem",message)); }
 @PostMapping("/login")
 @Operation(summary="Validar usuário e senha",responses={@ApiResponse(responseCode="200",description="Login válido"),@ApiResponse(responseCode="401",description="Credenciais inválidas ou conta bloqueada"),@ApiResponse(responseCode="400",description="JSON inválido")})
 public ResponseEntity<?> login(@Valid @RequestBody Credentials data) {
  return service.authenticate(data.username(),data.senha())!=null?response(200,"Login válido"):response(401,"Credenciais inválidas ou usuário bloqueado");
 }
 @PutMapping("/trocarsenha/{userid}")
 @Operation(summary="Trocar senha informando a atual; público",responses={@ApiResponse(responseCode="200",description="Senha alterada"),@ApiResponse(responseCode="401",description="Senha atual inválida, usuário inexistente ou bloqueado"),@ApiResponse(responseCode="400",description="Dados inválidos")})
 public ResponseEntity<?> change(@PathVariable String userid,@Valid @RequestBody PasswordChange data) {
  return service.changePassword(userid,data.atual(),data.nova())?response(200,"Senha alterada"):response(401,"Credenciais inválidas ou usuário bloqueado");
 }
 @PostMapping("/")
 @Operation(summary="Cadastrar usuário com ROLE_USER",security=@SecurityRequirement(name="basicAuth"),responses={@ApiResponse(responseCode="201",description="Criado"),@ApiResponse(responseCode="401",description="Não autenticado"),@ApiResponse(responseCode="403",description="Exige ROLE_MANUTENCAO"),@ApiResponse(responseCode="409",description="Username existente"),@ApiResponse(responseCode="400",description="Dados inválidos")})
 public ResponseEntity<?> create(@Valid @RequestBody Credentials data) { service.create(data.username(),data.senha(),"USER"); return response(201,"Usuário cadastrado"); }
 @PutMapping("/unlock/{userid}")
 @Operation(summary="Desbloquear usuário",security=@SecurityRequirement(name="basicAuth"),responses={@ApiResponse(responseCode="200",description="Desbloqueado"),@ApiResponse(responseCode="401",description="Não autenticado"),@ApiResponse(responseCode="403",description="Exige ROLE_ADMIN"),@ApiResponse(responseCode="404",description="Usuário inexistente")})
 public ResponseEntity<?> unlock(@PathVariable String userid) { return service.unlock(userid)?response(200,"Usuário desbloqueado"):response(404,"Usuário não encontrado"); }
 @GetMapping("/")
 @Operation(summary="Listar usuários sem expor senhas ou hashes",security=@SecurityRequirement(name="basicAuth"),responses={@ApiResponse(responseCode="200",description="Lista de usuários"),@ApiResponse(responseCode="401",description="Não autenticado"),@ApiResponse(responseCode="403",description="Exige ROLE_MANUTENCAO")})
 public List<Map<String,Object>> list() { return service.list(); }
}
