package br.com.bitewise.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Desabilita o CSRF, pois não usaremos sessions, e sim tokens JWT.
                .csrf(csrf -> csrf.disable())

                // 2. Configura a autorização de requisições HTTP
                .authorizeHttpRequests(auth -> auth
                        // Permite acesso público ao console do H2
                        .requestMatchers(toH2Console()).permitAll()

                        // Permite acesso público à nossa rota de teste "health"
                        .requestMatchers("/api/health").permitAll()

                        // (Futuramente, adicionaremos aqui: /api/auth/register, /api/auth/login)

                        // Exige autenticação para todas as outras rotas
                        .anyRequest().authenticated()
                );

        // 3. Permite que o H2 Console seja exibido em um <iframe> no navegador
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        // 4. Constrói a cadeia de filtros de segurança
        return http.build();
    }
}