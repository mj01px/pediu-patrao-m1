package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioFormDTO;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.erro.NegocioException;
import com.umc.pediupatrao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Telas de usuarios (somente ADMIN: SecurityConfig + @PreAuthorize do UsuarioService). */
@Controller
@RequestMapping("/usuarios")
public class UsuarioTelaController {

    private final UsuarioService usuarioService;

    public UsuarioTelaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return Telas.abrir(model, "usuarios/lista");
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        return formulario(model, new UsuarioFormDTO(), null, null);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, RedirectAttributes redirect) {
        return usuarioService.buscar(id)
                .map(usuario -> formulario(model, UsuarioFormDTO.de(usuario), id, null))
                .orElseGet(() -> {
                    redirect.addFlashAttribute("erro", "Usuário não encontrado.");
                    return "redirect:/usuarios";
                });
    }

    @PostMapping("/salvar")
    public String criar(@Valid @ModelAttribute("usuario") UsuarioFormDTO form, BindingResult validacao,
            Model model, RedirectAttributes redirect) {
        return gravar(form, validacao, null, model, redirect);
    }

    @PostMapping("/editar/{id}")
    public String alterar(@PathVariable String id, @Valid @ModelAttribute("usuario") UsuarioFormDTO form,
            BindingResult validacao, Model model, RedirectAttributes redirect) {
        return gravar(form, validacao, id, model, redirect);
    }

    @PostMapping("/deletar/{id}")
    public String remover(@PathVariable String id, RedirectAttributes redirect) {
        try {
            usuarioService.remover(id);
            redirect.addFlashAttribute("sucesso", "Usuário removido.");
        } catch (NegocioException | NaoEncontradoException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/usuarios";
    }

    // Criar e alterar seguem o mesmo caminho; id nulo significa criacao
    private String gravar(UsuarioFormDTO form, BindingResult validacao, String id,
            Model model, RedirectAttributes redirect) {
        if (validacao.hasErrors()) {
            return formulario(model, form, id, validacao.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            if (id == null) {
                usuarioService.criar(form);
            } else {
                usuarioService.alterar(id, form);
            }
        } catch (NegocioException | NaoEncontradoException ex) {
            return formulario(model, form, id, ex.getMessage());
        }
        redirect.addFlashAttribute("sucesso", id == null ? "Usuário criado." : "Usuário atualizado.");
        return "redirect:/usuarios";
    }

    private String formulario(Model model, UsuarioFormDTO form, String id, String erro) {
        form.setSenha(null); // a senha digitada nunca volta para o HTML
        model.addAttribute("usuario", form);
        model.addAttribute("usuarioId", id);
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("erro", erro);
        return Telas.abrir(model, "usuarios/form");
    }
}
