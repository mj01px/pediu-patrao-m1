package com.umc.pediupatrao.seguranca;

/**
 * Expressoes de autorizacao usadas no @PreAuthorize dos services.
 * Ficam todas aqui para a matriz de permissoes ser lida num lugar so.
 */
public final class RegrasAcesso {

    public static final String QUALQUER_PERFIL = "hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')";
    public static final String SO_ADMIN = "hasRole('ADMIN')";
    public static final String SO_GERENTE = "hasRole('GERENTE')";
    public static final String SO_ATENDENTE = "hasRole('ATENDENTE')";
    /** Quem opera o balcao: registra pedidos e muda status. */
    public static final String OPERACAO = "hasAnyRole('GERENTE', 'ATENDENTE')";
    /** Quem le a auditoria e cuida do cardapio. */
    public static final String SUPERVISAO = "hasAnyRole('ADMIN', 'GERENTE')";

    private RegrasAcesso() {
    }
}
