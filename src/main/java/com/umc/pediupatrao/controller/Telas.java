package com.umc.pediupatrao.controller;

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
}
