package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.erro.NegocioException;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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

    private static final DateTimeFormatter DATA_DO_TOPO =
            DateTimeFormatter.ofPattern("EEEE · dd/MM/yyyy · HH:mm", Locale.of("pt", "BR"));

    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final PedidoService pedidoService;
    private final Clock relogio;

    public HomeController(ClienteService clienteService, ProdutoService produtoService,
            PedidoService pedidoService, Clock relogio) {
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.pedidoService = pedidoService;
        this.relogio = relogio;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Pedido> pedidos = pedidoService.listar(); // ja vem do mais novo para o mais antigo
        model.addAttribute("hoje", DATA_DO_TOPO.format(agora));
        model.addAttribute("saudacao", agora.getHour() < 12 ? "Bom dia" : agora.getHour() < 18 ? "Boa tarde" : "Boa noite");
        model.addAttribute("colunas", PedidoTelaController.Coluna.emAndamento(pedidos));
        model.addAttribute("ultimos", pedidos.stream().limit(6).toList());
        return Telas.abrir(model, "home");
    }

    // ========================
    // PRODUTOS
    // ========================
    @GetMapping("/produtos")
    public String produtos(Model model) {
        List<Produto> produtos = produtoService.listar();
        long aVenda = produtos.stream().filter(Produto::disponivelParaVenda).count();
        model.addAttribute("grupos", Telas.porTipo(produtos));
        model.addAttribute("aVenda", aVenda);
        model.addAttribute("pausados", produtos.size() - aVenda);
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
