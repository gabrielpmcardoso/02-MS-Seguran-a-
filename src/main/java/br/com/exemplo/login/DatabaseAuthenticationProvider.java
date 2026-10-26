package br.com.exemplo.login;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
@Component
public class DatabaseAuthenticationProvider implements AuthenticationProvider {
 private final UserService service;
 public DatabaseAuthenticationProvider(UserService service) { this.service=service; }
 @Override public Authentication authenticate(Authentication authentication) {
  // A exceção ocorre DEPOIS do commit, preservando a falha e o bloqueio no banco.
  var user=service.authenticate(authentication.getName(),String.valueOf(authentication.getCredentials()));
  if(user==null) throw new BadCredentialsException("Credenciais inválidas ou usuário bloqueado");
  return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());
 }
 @Override public boolean supports(Class<?> type) { return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type); }
}
