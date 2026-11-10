package com.umc.pediupatrao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tela do dashboard. Ela so traz a estrutura: os numeros vem da /api/dashboard via fetch (RF06). */
@Controller
public class DashboardTelaController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return Telas.abrir(model, "dashboard/painel");
    }
}
