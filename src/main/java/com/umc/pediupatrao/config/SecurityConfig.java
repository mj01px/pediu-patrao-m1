package com.umc.pediupatrao.config;

import com.umc.pediupatrao.seguranca.RespostaNegadaApi;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Duas cadeias de seguranca:
 *  1) /api/**  : respostas JSON, aceita sessao (telas) ou HTTP Basic (curl/Postman), nega o que nao estiver listado;
 *  2) telas    : login por formulario, CSRF obrigatorio.
 * Alem disso, @EnableMethodSecurity liga o @PreAuthorize dos services (segunda barreira).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String GERENTE = "GERENTE";
    private static final String ATENDENTE = "ATENDENTE";

    private static final String[] RECURSOS_PUBLICOS = {
        "/login", "/error", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**"
    };

    private final UsuarioDetailsService usuarioDetailsService;
    private final RespostaNegadaApi respostaNegadaApi;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService, RespostaNegadaApi respostaNegadaApi) {
        this.usuarioDetailsService = usuarioDetailsService;
        this.respostaNegadaApi = respostaNegadaApi;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain cadeiaApi(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers("/api/usuarios/**").hasRole(ADMIN)
                        .requestMatchers("/api/auditoria/**").hasAnyRole(ADMIN, GERENTE)

                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/desconto").hasRole(GERENTE)
                        .requestMatchers(HttpMethod.POST, "/api/pedidos/*/cancelamento").hasRole(GERENTE)
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole(GERENTE, ATENDENTE)
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/status").hasAnyRole(GERENTE, ATENDENTE)
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**").hasAnyRole(ADMIN, GERENTE, ATENDENTE)

                        .requestMatchers(HttpMethod.GET, "/api/clientes/**").hasAnyRole(GERENTE, ATENDENTE)
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/*").hasRole(GERENTE)
                        .requestMatchers(HttpMethod.POST, "/api/clientes").hasRole(ATENDENTE)
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/*").hasRole(ATENDENTE)

                        .requestMatchers(HttpMethod.GET, "/api/produtos/**").authenticated()
                        .requestMatchers("/api/produtos/**").hasAnyRole(ADMIN, GERENTE)

                        // Lista branca: qualquer rota de API nao prevista acima e negada
                        .anyRequest().denyAll()
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(respostaNegadaApi))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(respostaNegadaApi)
                        .accessDeniedHandler(respostaNegadaApi)
                )
                // CSRF continua valendo para as chamadas feitas pelas telas (cookie de sessao).
                // Chamada com HTTP Basic nao depende de cookie, entao nao ha como ser forjada por outro site.
                .csrf(csrf -> csrf.ignoringRequestMatchers(SecurityConfig::autenticaComBasic));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain cadeiaTelas(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(RECURSOS_PUBLICOS).permitAll()
                        .requestMatchers("/usuarios/**").hasRole(ADMIN)
                        .requestMatchers("/auditoria/**").hasAnyRole(ADMIN, GERENTE)
                        .requestMatchers("/pedidos/novo").hasAnyRole(GERENTE, ATENDENTE)
                        .requestMatchers("/clientes/novo", "/clientes/salvar", "/clientes/editar/**").hasRole(ATENDENTE)
                        .requestMatchers("/clientes/**").hasAnyRole(GERENTE, ATENDENTE)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }

    private static boolean autenticaComBasic(HttpServletRequest request) {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        return cabecalho != null && cabecalho.regionMatches(true, 0, "Basic ", 0, 6);
    }
}
