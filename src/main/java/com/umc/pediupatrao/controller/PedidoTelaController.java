package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Telas de pedidos: quadro da cozinha, novo pedido e detalhe. */
@Controller
public class PedidoTelaController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    public PedidoTelaController(PedidoService pedidoService, ClienteService clienteService,
            ProdutoService produtoService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
    }

    /** Uma coluna do quadro: titulo, cor e os pedidos que estao naquelas etapas. */
    public static class Coluna {
        private final String titulo;
        private final String cor;
        private final List<Pedido> pedidos;

        Coluna(String titulo, String cor, List<Pedido> todos, Set<StatusPedido> etapas) {
            this.titulo = titulo;
            this.cor = cor;
            this.pedidos = todos.stream().filter(p -> etapas.contains(p.getStatus())).toList();
        }

        public String getTitulo() {
            return titulo;
        }

        public String getCor() {
            return cor;
        }

        public List<Pedido> getPedidos() {
            return pedidos;
        }
    }

    @GetMapping("/pedidos")
    public String quadro(Model model) {
        List<Pedido> todos = pedidoService.listar();
        model.addAttribute("colunas", List.of(
                new Coluna("Recebidos", "secondary", todos, EnumSet.of(StatusPedido.RECEBIDO)),
                new Coluna("No forno", "warning", todos, EnumSet.of(StatusPedido.EM_PREPARACAO)),
                new Coluna("Prontos", "success", todos, EnumSet.of(StatusPedido.PRONTO)),
                new Coluna("Saíram", "info", todos, EnumSet.of(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO))));
        model.addAttribute("encerrados", new Coluna("Encerrados", "dark", todos,
                EnumSet.of(StatusPedido.FINALIZADO, StatusPedido.CANCELADO)).getPedidos());
        return Telas.abrir(model, "pedidos/quadro");
    }

    @GetMapping("/pedidos/novo")
    public String novo(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("produtos", produtoService.listarDisponiveis());
        return Telas.abrir(model, "pedidos/novo");
    }

    @GetMapping("/pedidos/{id}")
    public String detalhe(@PathVariable String id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("pedido", pedidoService.buscar(id));
        } catch (NaoEncontradoException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/pedidos";
        }
        return Telas.abrir(model, "pedidos/detalhe");
    }
}
