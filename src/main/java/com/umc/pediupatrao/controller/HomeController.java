package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.erro.NegocioException;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas gerais, clientes e produtos. Pedidos, usuarios e auditoria ganharam
 * controllers de tela proprios (PedidoTelaController, UsuarioTelaController, AuditoriaTelaController).
 */
@Controller
public class HomeController {

    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    public HomeController(ClienteService clienteService, ProdutoService produtoService) {
        this.clienteService = clienteService;
        this.produtoService = produtoService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        return Telas.abrir(model, "home");
    }

    // ========================
    // PRODUTOS
    // ========================
    @GetMapping("/produtos")
    public String produtos(Model model) {
        model.addAttribute("produtos", produtoService.listar());
        return Telas.abrir(model, "produtos/lista");
    }

    // ========================
    // CLIENTES (a lista e carregada via /api/clientes)
    // ========================
    @GetMapping("/clientes")
    public String clientes(Model model) {
        return Telas.abrir(model, "clientes/lista");
    }

    @GetMapping("/clientes/novo")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        return Telas.abrir(model, "clientes/form");
    }

    @PostMapping("/clientes/salvar")
    public String salvarCliente(@ModelAttribute Cliente cliente, Model model, RedirectAttributes redirect) {
        try {
            clienteService.cadastrar(cliente);
        } catch (NegocioException ex) {
            cliente.setId(null);
            model.addAttribute("erro", ex.getMessage());
            return Telas.abrir(model, "clientes/form");
        }
        redirect.addFlashAttribute("sucesso", "Cliente cadastrado.");
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    public String editarClienteForm(@PathVariable String id, Model model, RedirectAttributes redirect) {
        return clienteService.buscar(id)
                .map(cliente -> {
                    model.addAttribute("cliente", cliente);
                    return Telas.abrir(model, "clientes/form");
                })
                .orElseGet(() -> {
                    redirect.addFlashAttribute("erro", "Cliente não encontrado.");
                    return "redirect:/clientes";
                });
    }

    @PostMapping("/clientes/editar/{id}")
    public String atualizarCliente(@PathVariable String id, @ModelAttribute Cliente cliente,
            Model model, RedirectAttributes redirect) {
        try {
            clienteService.alterar(id, cliente);
        } catch (NegocioException | NaoEncontradoException ex) {
            cliente.setId(id);
            model.addAttribute("erro", ex.getMessage());
            return Telas.abrir(model, "clientes/form");
        }
        redirect.addFlashAttribute("sucesso", "Cliente atualizado.");
        return "redirect:/clientes";
    }

    // ========================
    // OUTRAS ROTAS
    // ========================
    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        return Telas.abrir(model, "configuracoes");
    }

    @GetMapping("/teste")
    public String teste() {
        return "teste";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}
