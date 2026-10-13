package com.umc.pediupatrao.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Compara dois objetos do mesmo tipo campo a campo, a partir de uma lista declarada
 * uma unica vez. Ex.: ComparadorCampos.para(Cliente.class).campo("logradouro", Cliente::getLogradouro)
 *
 * @param <T> tipo comparado
 */
public final class ComparadorCampos<T> {

    private record Campo<T>(String nome, Function<T, ?> leitor, boolean sensivel) {
    }

    private final List<Campo<T>> campos = new ArrayList<>();

    private ComparadorCampos() {
    }

    public static <T> ComparadorCampos<T> para(Class<T> tipo) {
        return new ComparadorCampos<>();
    }

    public ComparadorCampos<T> campo(String nome, Function<T, ?> leitor) {
        campos.add(new Campo<>(nome, leitor, false));
        return this;
    }

    /** Campo com dado pessoal: aparece mascarado na auditoria. */
    public ComparadorCampos<T> sensivel(String nome, Function<T, String> leitor) {
        campos.add(new Campo<>(nome, leitor, true));
        return this;
    }

    /** "antes" nulo significa inclusao: todos os campos preenchidos aparecem como novos. */
    public Mudancas comparar(T antes, T depois) {
        Mudancas mudancas = Mudancas.nova();
        for (Campo<T> campo : campos) {
            Object de = antes == null ? null : campo.leitor().apply(antes);
            Object para = depois == null ? null : campo.leitor().apply(depois);
            if (campo.sensivel()) {
                mudancas.registrarMascarado(campo.nome(), (String) de, (String) para);
            } else {
                mudancas.registrar(campo.nome(), de, para);
            }
        }
        return mudancas;
    }
}
