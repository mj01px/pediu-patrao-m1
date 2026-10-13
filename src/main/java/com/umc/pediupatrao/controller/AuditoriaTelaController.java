package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditoriaTelaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaTelaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    public String trilha(@RequestParam(required = false) String entidade,
            @RequestParam(required = false) String registroId, Model model) {
        model.addAttribute("eventos", auditoriaService.pesquisar(entidade, registroId));
        model.addAttribute("entidades", List.of("Pedido", "Cliente", "Usuario"));
        model.addAttribute("entidade", entidade);
        model.addAttribute("registroId", registroId);
        return Telas.abrir(model, "auditoria/trilha");
    }
}
