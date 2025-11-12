package academic_festival.gyeonggi_go.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger", "v2/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "api-docs", "/api-docs/**", "/configuration/security", "/configuration/ui", "/swagger-resources/**", "/webjars/**",
                                "/home", "/places/**", "/chatbot/**").permitAll()
                        .anyRequest().authenticated() // 나머지 요청은 인증 필요
                );
        return http.build();
    }
}