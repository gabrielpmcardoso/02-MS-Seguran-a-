package br.com.exemplo.login;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
@Service
public class UserService {
 private final JdbcTemplate jdbc;
 private final JdbcUserDetailsManager users;
 private final PasswordEncoder encoder;
 private final String dummy;
 public UserService(JdbcTemplate jdbc, JdbcUserDetailsManager users, PasswordEncoder encoder) {
  this.jdbc=jdbc; this.users=users; this.encoder=encoder; this.dummy=encoder.encode("timing-placeholder");
 }
 private boolean lockUser(String username) {
  return !jdbc.queryForList("SELECT username FROM usuarios WHERE username=? FOR UPDATE",String.class,username).isEmpty();
 }
 private boolean locked(String username) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT locked FROM login_attempts WHERE username=?",Boolean.class,username)); }
 private void metric(boolean ok) { jdbc.update("UPDATE auth_metrics SET "+(ok?"valid_logins=valid_logins+1":"invalid_logins=invalid_logins+1")+" WHERE id=1"); }
 private void failure(String username) {
  jdbc.update("UPDATE login_attempts SET failed_attempts=failed_attempts+1,last_failed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE username=?",username);
  jdbc.update("UPDATE login_attempts SET locked=TRUE,locked_at=COALESCE(locked_at,CURRENT_TIMESTAMP) WHERE username=? AND failed_attempts>=2",username);
 }
 @Transactional
 public UserDetails authenticate(String username,String password) {
  if(!lockUser(username)) { encoder.matches(password,dummy); metric(false); return null; }
  var user=users.loadUserByUsername(username);
  boolean matches=password.getBytes(StandardCharsets.UTF_8).length<=72 && encoder.matches(password,user.getPassword());
  if(!user.isEnabled() || locked(username) || !matches) { failure(username); metric(false); return null; }
  jdbc.update("UPDATE login_attempts SET failed_attempts=0,updated_at=CURRENT_TIMESTAMP WHERE username=?",username);
  metric(true);
  return user;
 }
 @Transactional
 public void create(String username,String password,String... roles) {
  validatePassword(password);
  users.createUser(User.withUsername(username).password(encoder.encode(password)).roles(roles).build());
  jdbc.update("INSERT INTO login_attempts(username) VALUES (?)",username);
 }
 @Transactional
 public boolean changePassword(String username,String current,String next) {
  validatePassword(next);
  if(!lockUser(username)) { encoder.matches(current,dummy); return false; }
  var user=users.loadUserByUsername(username);
  if(locked(username) || !user.isEnabled()) return false;
  if(current.getBytes(StandardCharsets.UTF_8).length>72 || !encoder.matches(current,user.getPassword())) { failure(username); return false; }
  jdbc.update("UPDATE usuarios SET senha=? WHERE username=?",encoder.encode(next),username);
  // Troca de senha não desbloqueia a conta e não é contabilizada como login.
  return true;
 }
 @Transactional
 public boolean unlock(String username) {
  if(!lockUser(username)) return false;
  jdbc.update("UPDATE login_attempts SET failed_attempts=0,locked=FALSE,last_failed_at=NULL,locked_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE username=?",username);
  return true;
 }
 public List<Map<String,Object>> list() {
  return jdbc.query("SELECT u.username,l.locked,l.failed_attempts FROM usuarios u JOIN login_attempts l ON l.username=u.username ORDER BY u.username",(rs,n)->{
   Map<String,Object> m=new LinkedHashMap<>(); String username=rs.getString(1);
   m.put("username",username); m.put("bloqueado",rs.getBoolean(2)); m.put("tentativasFalhas",rs.getInt(3));
   m.put("roles",jdbc.queryForList("SELECT authority FROM authorities WHERE username=? ORDER BY authority",String.class,username)); return m;
  });
 }
 private void validatePassword(String password) {
  if(password==null || password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("Senha deve ter entre 1 e 72 bytes UTF-8");
 }
}
