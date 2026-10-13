package com.umc.pediupatrao.erro;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz excecoes das APIs para ProblemDetail (RFC 7807): {type, title, status, detail, codigo}.
 * Vale so para @RestController; as telas tratam erro nos proprios controllers.
 */
@RestControllerAdvice(annotations = RestController.class)
public class TratadorErrosApi {

    private static final Logger log = LoggerFactory.getLogger(TratadorErrosApi.class);

    @ExceptionHandler(NegocioException.class)
    public ProblemDetail negocio(NegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regra-de-negocio", ex.getMessage());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ProblemDetail naoEncontrado(NaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", ex.getMessage());
    }

    // Lancada pelo @PreAuthorize dos services
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail negado(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado no service: usuario={} {} {}",
                request.getRemoteUser(), request.getMethod(), request.getRequestURI());
        return problema(HttpStatus.FORBIDDEN, "acesso-negado", "Seu perfil não pode executar esta operação.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException ex) {
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "dados-invalidos", "Há campos inválidos.");
        problema.setProperty("erros", erros);
        return problema;
    }

    // JSON quebrado ou valor fora do enum (status=VOANDO, perfil=ROOT)
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail formato(Exception ex) {
        return problema(HttpStatus.BAD_REQUEST, "formato-invalido", "Formato ou valor inválido na requisição.");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail concorrencia(OptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "alterado-por-outro",
                "Outra pessoa alterou este registro agora. Recarregue e tente de novo.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex, HttpServletRequest request) {
        log.error("Falha inesperada em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno. Tente novamente.");
    }

    private static ProblemDetail problema(HttpStatus status, String codigo, String mensagem) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensagem);
        problema.setType(URI.create("urn:pediupatrao:" + codigo));
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
