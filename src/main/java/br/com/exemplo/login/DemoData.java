package br.com.exemplo.login;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
@Component
@ConditionalOnProperty(name="app.demo",havingValue="true")
public class DemoData implements ApplicationRunner {
 private final UserService service; private final JdbcUserDetailsManager users;
 public DemoData(UserService service,JdbcUserDetailsManager users){this.service=service;this.users=users;}
 public void run(ApplicationArguments args) {
  if(!users.userExists("admin")) service.create("admin","Admin123!","ADMIN");
  if(!users.userExists("manutencao")) service.create("manutencao","Manut123!","MANUTENCAO");
  if(!users.userExists("aluno")) service.create("aluno","Aluno123!","USER");
 }
}
