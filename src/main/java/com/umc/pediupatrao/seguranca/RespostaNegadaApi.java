package com.umc.pediupatrao.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/**
 * 401 e 403 da API em JSON (ProblemDetail). Sem isto, a API devolveria a tela de login em HTML.
 * Nao envia WWW-Authenticate, entao o navegador nunca abre a janela de login Basic.
 */
@Component
public class RespostaNegadaApi implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RespostaNegadaApi.class);

    private final ObjectMapper json;

    public RespostaNegadaApi(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException excecao) throws IOException {
        responder(request, response, HttpStatus.UNAUTHORIZED, "nao-autenticado",
                "Faça login para usar este recurso.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException excecao) throws IOException {
        // O filtro de CSRF roda antes da autorizacao: separa os casos para a mensagem nao enganar
        if (excecao instanceof CsrfException) {
            log.warn("Requisição sem token CSRF válido: {} {}", request.getMethod(), request.getRequestURI());
            responder(request, response, HttpStatus.FORBIDDEN, "csrf-invalido",
                    "Token CSRF ausente ou inválido. Recarregue a página.");
            return;
        }
        log.warn("Acesso negado: usuario={} {} {}", request.getRemoteUser(), request.getMethod(), request.getRequestURI());
        responder(request, response, HttpStatus.FORBIDDEN, "acesso-negado",
                "Seu perfil não pode executar esta operação.");
    }

    private void responder(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String codigo, String mensagem) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensagem);
        problema.setType(URI.create("urn:pediupatrao:" + codigo));
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("codigo", codigo);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(problema));
    }
}
