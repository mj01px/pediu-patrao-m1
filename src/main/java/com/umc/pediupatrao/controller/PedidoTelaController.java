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

    /** Uma coluna do quadro: titulo, status que da a cor (classe s-STATUS no CSS) e os pedidos daquelas etapas. */
    public static class Coluna {
        private final String titulo;
        private final StatusPedido estilo;
        private final List<Pedido> pedidos;

        Coluna(String titulo, StatusPedido estilo, List<Pedido> todos, Set<StatusPedido> etapas) {
            this.titulo = titulo;
            this.estilo = estilo;
            this.pedidos = todos.stream().filter(p -> etapas.contains(p.getStatus())).toList();
        }

        /** As quatro colunas de pedidos em andamento, usadas no quadro e nos contadores do inicio. */
        static List<Coluna> emAndamento(List<Pedido> todos) {
            return List.of(
                    new Coluna("Recebidos", StatusPedido.RECEBIDO, todos, EnumSet.of(StatusPedido.RECEBIDO)),
                    new Coluna("No forno", StatusPedido.EM_PREPARACAO, todos, EnumSet.of(StatusPedido.EM_PREPARACAO)),
                    new Coluna("Prontos", StatusPedido.PRONTO, todos, EnumSet.of(StatusPedido.PRONTO)),
                    new Coluna("Saíram", StatusPedido.SAIU_PARA_ENTREGA, todos,
                            EnumSet.of(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO)));
        }

        public String getTitulo() {
            return titulo;
        }

        public StatusPedido getEstilo() {
            return estilo;
        }

        public List<Pedido> getPedidos() {
            return pedidos;
        }
    }

    @GetMapping("/pedidos")
    public String quadro(Model model) {
        List<Pedido> todos = pedidoService.listar();
        List<Coluna> colunas = Coluna.emAndamento(todos);
        model.addAttribute("colunas", colunas);
        model.addAttribute("emAndamento", colunas.stream().mapToInt(c -> c.getPedidos().size()).sum());
        model.addAttribute("encerrados", todos.stream()
                .filter(p -> p.getStatus() == StatusPedido.FINALIZADO || p.getStatus() == StatusPedido.CANCELADO)
                .toList());
        return Telas.abrir(model, "pedidos/quadro");
    }

    @GetMapping("/pedidos/novo")
    public String novo(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("grupos", Telas.porTipo(produtoService.listarDisponiveis()));
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
