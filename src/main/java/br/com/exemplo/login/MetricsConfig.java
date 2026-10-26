package br.com.exemplo.login;
import io.micrometer.core.instrument.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
@Configuration
public class MetricsConfig {
 @Bean io.micrometer.core.instrument.binder.MeterBinder authMetrics(JdbcTemplate jdbc) {
  return registry->{
   FunctionCounter.builder("auth.logins.valid",jdbc,j->j.queryForObject("SELECT valid_logins FROM auth_metrics WHERE id=1",Long.class).doubleValue()).description("Autenticações válidas acumuladas no banco").register(registry);
   FunctionCounter.builder("auth.logins.invalid",jdbc,j->j.queryForObject("SELECT invalid_logins FROM auth_metrics WHERE id=1",Long.class).doubleValue()).description("Autenticações inválidas acumuladas no banco").register(registry);
   Gauge.builder("auth.users.locked",jdbc,j->j.queryForObject("SELECT COUNT(*) FROM login_attempts WHERE locked=TRUE",Long.class).doubleValue()).description("Usuários atualmente bloqueados").register(registry);
  };
 }
}
