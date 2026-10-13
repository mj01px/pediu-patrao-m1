package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Trilha de auditoria: so leitura. POST, PUT e DELETE aqui respondem 405. */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    // Ex.: GET /api/auditoria?entidade=Cliente&registroId=6710...
    @GetMapping
    public List<EventoAuditoria> pesquisar(@RequestParam(required = false) String entidade,
            @RequestParam(required = false) String registroId) {
        return auditoriaService.pesquisar(entidade, registroId);
    }
}
