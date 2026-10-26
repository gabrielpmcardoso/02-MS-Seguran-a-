package br.com.exemplo.login;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean JdbcUserDetailsManager users(DataSource ds) {
  var m = new JdbcUserDetailsManager(ds);
  m.setUsersByUsernameQuery("SELECT u.username,u.senha,(u.enabled AND NOT COALESCE(l.locked,FALSE)) FROM usuarios u LEFT JOIN login_attempts l ON l.username=u.username WHERE u.username=?");
  m.setAuthoritiesByUsernameQuery("SELECT username,authority FROM authorities WHERE username=?");
  m.setCreateUserSql("INSERT INTO usuarios(username,senha,enabled) VALUES (?,?,?)");
  m.setUpdateUserSql("UPDATE usuarios SET senha=?,enabled=? WHERE username=?");
  m.setChangePasswordSql("UPDATE usuarios SET senha=? WHERE username=?");
  m.setUserExistsSql("SELECT username FROM usuarios WHERE username=?");
  return m;
 }
 @Bean @Order(1) SecurityFilterChain publicEndpoints(HttpSecurity h) throws Exception {
  return h.securityMatcher("/login","/trocarsenha/**","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/actuator/**")
   .csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.anyRequest().permitAll()).build();
 }
 @Bean SecurityFilterChain protectedEndpoints(HttpSecurity h, DatabaseAuthenticationProvider provider) throws Exception {
  return h.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authenticationProvider(provider)
   .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.PUT,"/unlock/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.POST,"/").hasRole("MANUTENCAO")
    .requestMatchers(HttpMethod.GET,"/").hasRole("MANUTENCAO").anyRequest().denyAll())
   .httpBasic(b->b.authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setHeader("WWW-Authenticate","Basic realm=\"login-api\"");res.setContentType("application/json");res.getWriter().write("{\"mensagem\":\"Credenciais invalidas ou usuario bloqueado\"}");}))
   .exceptionHandling(e->e.accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"mensagem\":\"Role insuficiente\"}");})).build();
 }
 @Bean OpenAPI openAPI() {
  return new OpenAPI().info(new Info().title("API REST de Login").version("1.0.0").description("userid = username. Endpoints protegidos usam HTTP Basic; /login valida credenciais sem gerar token. Bloqueio após duas falhas consecutivas."))
   .components(new Components().addSecuritySchemes("basicAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
 }
}
