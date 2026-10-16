package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Produto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ui.Model;

/** Atalho para o padrao do projeto: tudo renderiza dentro do layout.html. */
final class Telas {

    private Telas() {
    }

    /** Ex.: Telas.abrir(model, "pedidos/quadro") carrega o fragmento "pedidos/quadro :: content". */
    static String abrir(Model model, String template) {
        model.addAttribute("content", template + " :: content");
        return "layout";
    }

    /** Agrupa o cardapio por tipo (Pizza, Esfiha...) mantendo a ordem em que os produtos chegaram. */
    static Map<String, List<Produto>> porTipo(List<Produto> produtos) {
        return produtos.stream().collect(Collectors.groupingBy(
                p -> p.getTipo() == null || p.getTipo().isBlank() ? "Outros" : p.getTipo(),
                LinkedHashMap::new, Collectors.toList()));
    }
}
