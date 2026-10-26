package br.com.exemplo.login;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import java.util.concurrent.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1","app.demo=false","management.server.port=0"})
@AutoConfigureMockMvc
class LoginIntegrationTest {
 @Autowired MockMvc mvc;
 @org.springframework.boot.test.web.server.LocalManagementPort int managementPort;
 @Autowired JdbcTemplate jdbc;
 @Autowired UserService service;
 @Autowired JdbcUserDetailsManager users;
 @Autowired PasswordEncoder encoder;
 @BeforeEach void setup() {
  jdbc.update("DELETE FROM authorities"); jdbc.update("DELETE FROM login_attempts"); jdbc.update("DELETE FROM usuarios");
  jdbc.update("UPDATE auth_metrics SET valid_logins=0,invalid_logins=0");
  service.create("admin","Admin123!","ADMIN"); service.create("manutencao","Manut123!","MANUTENCAO"); service.create("aluno","Aluno123!","USER");
 }
 void login(String username,String senha,int status) throws Exception {
  mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"senha\":\""+senha+"\"}")).andExpect(status().is(status));
 }
 long metric(String col) { return jdbc.queryForObject("SELECT "+col+" FROM auth_metrics WHERE id=1",Long.class); }
 boolean locked() { return jdbc.queryForObject("SELECT locked FROM login_attempts WHERE username='aluno'",Boolean.class); }
 @Test void validAndInvalidMetrics() throws Exception { login("aluno","Aluno123!",200); login("aluno","erro",401); login("desconhecido","erro",401); assertThat(metric("valid_logins")).isEqualTo(1); assertThat(metric("invalid_logins")).isEqualTo(2); }
 @Test void secondFailureLocksAndCorrectPasswordCannotBypass() throws Exception { login("aluno","erro",401); assertThat(locked()).isFalse(); login("aluno","erro",401); assertThat(locked()).isTrue(); login("aluno","Aluno123!",401); assertThat(users.loadUserByUsername("aluno").isEnabled()).isFalse(); }
 @Test void successResetsConsecutiveFailures() throws Exception { login("aluno","erro",401); login("aluno","Aluno123!",200); login("aluno","erro",401); assertThat(locked()).isFalse(); }
 @Test void maintenanceCanCreateAndListWithoutPasswords() throws Exception {
  mvc.perform(post("/").with(httpBasic("manutencao","Manut123!")).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"novo\",\"senha\":\"Nova123!\"}")).andExpect(status().isCreated());
  mvc.perform(get("/").with(httpBasic("manutencao","Manut123!"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].senha").doesNotExist()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("$2a$"))));
  assertThat(users.loadUserByUsername("novo").getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
 }
 @Test void publicAndRolesAreEnforced() throws Exception {
  mvc.perform(get("/")).andExpect(status().isUnauthorized());
  mvc.perform(get("/").with(httpBasic("aluno","Aluno123!"))).andExpect(status().isForbidden());
  mvc.perform(post("/").with(httpBasic("admin","Admin123!")).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"novo\",\"senha\":\"abc\"}")).andExpect(status().isForbidden());
  mvc.perform(put("/unlock/aluno").with(httpBasic("manutencao","Manut123!"))).andExpect(status().isForbidden());
 }
 @Test void adminUnlockResetsState() throws Exception {
  login("aluno","erro",401); login("aluno","erro",401);
  mvc.perform(put("/unlock/aluno").with(httpBasic("admin","Admin123!"))).andExpect(status().isOk());
  assertThat(locked()).isFalse(); assertThat(jdbc.queryForObject("SELECT failed_attempts FROM login_attempts WHERE username='aluno'",Integer.class)).isZero(); login("aluno","Aluno123!",200);
 }
 @Test void publicPasswordChangeRequiresCurrentPassword() throws Exception {
  mvc.perform(put("/trocarsenha/aluno").contentType(MediaType.APPLICATION_JSON).content("{\"atual\":\"errada\",\"nova\":\"Nova123!\"}")).andExpect(status().isUnauthorized());
  mvc.perform(put("/trocarsenha/aluno").contentType(MediaType.APPLICATION_JSON).content("{\"atual\":\"Aluno123!\",\"nova\":\"Nova123!\"}")).andExpect(status().isOk());
  assertThat(encoder.matches("Nova123!",users.loadUserByUsername("aluno").getPassword())).isTrue(); login("aluno","Nova123!",200);
 }
 @Test void passwordChangeCannotUnlock() throws Exception {
  login("aluno","x",401); login("aluno","x",401);
  mvc.perform(put("/trocarsenha/aluno").contentType(MediaType.APPLICATION_JSON).content("{\"atual\":\"Aluno123!\",\"nova\":\"Nova123!\"}")).andExpect(status().isUnauthorized()); assertThat(locked()).isTrue();
 }
 @Test void invalidBodyAndDuplicateAreHandled() throws Exception {
  mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
  mvc.perform(post("/").with(httpBasic("manutencao","Manut123!")).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"aluno\",\"senha\":\"x\"}")).andExpect(status().isConflict());
  mvc.perform(post("/").with(httpBasic("manutencao","Manut123!")).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"novo\",\"senha\":\"x\",\"roles\":[\"ADMIN\"]}")).andExpect(status().isBadRequest());
 }
 @Test void concurrentFailuresAreNotLost() throws Exception {
  var pool=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
  try { List<Future<UserDetailsResult>> results=new ArrayList<>();
   for(int i=0;i<2;i++) results.add(pool.submit(()->{gate.await(); return new UserDetailsResult(service.authenticate("aluno","errada")==null);}));
   gate.countDown(); for(var result:results) assertThat(result.get(10,TimeUnit.SECONDS).failed()).isTrue();
   assertThat(locked()).isTrue(); assertThat(metric("invalid_logins")).isEqualTo(2);
  } finally { pool.shutdownNow(); }
 }
 record UserDetailsResult(boolean failed) {}
 @Test void basicAuthHonorsAndRegistersLockout() throws Exception {
  mvc.perform(get("/").with(httpBasic("manutencao","erro"))).andExpect(status().isUnauthorized());
  mvc.perform(get("/").with(httpBasic("manutencao","erro"))).andExpect(status().isUnauthorized());
  mvc.perform(get("/").with(httpBasic("manutencao","Manut123!"))).andExpect(status().isUnauthorized());
 }
 @Test void docsAndPrometheusArePresent() throws Exception {
  mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/login'].post").exists());
  String metrics=new org.springframework.web.client.RestTemplate().getForObject("http://localhost:"+managementPort+"/actuator/prometheus",String.class);
  assertThat(metrics).contains("auth_users_locked","auth_logins_valid_total","auth_logins_invalid_total");
 }
}
