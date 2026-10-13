package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioDTO;
import com.umc.pediupatrao.dto.UsuarioFormDTO;
import com.umc.pediupatrao.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** API de usuarios: antes publica, agora so ADMIN. Nunca devolve senha. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioDTO> listarTodos() {
        return usuarioService.listar().stream().map(UsuarioDTO::de).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.of(usuarioService.buscar(id).map(UsuarioDTO::de));
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> criarUsuario(@Valid @RequestBody UsuarioFormDTO form) {
        UsuarioDTO criado = UsuarioDTO.de(usuarioService.criar(form));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri()).body(criado);
    }

    @PutMapping("/{id}")
    public UsuarioDTO atualizarUsuario(@PathVariable String id, @Valid @RequestBody UsuarioFormDTO form) {
        return UsuarioDTO.de(usuarioService.alterar(id, form));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable String id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
