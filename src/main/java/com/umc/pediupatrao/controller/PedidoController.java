package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CancelamentoDTO;
import com.umc.pediupatrao.dto.DescontoDTO;
import com.umc.pediupatrao.dto.NovoPedidoDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listar();
    }

    @GetMapping("/{id}")
    public Pedido detalhar(@PathVariable String id) {
        return pedidoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@Valid @RequestBody NovoPedidoDTO dados) {
        Pedido pedido = pedidoService.registrar(dados);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(pedido.getId()).toUri()).body(pedido);
    }

    // Mesmo formato da versao original: PUT /api/pedidos/{id}/status?status=PRONTO
    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam StatusPedido status) {
        return pedidoService.avancar(id, status);
    }

    // PUT porque define o desconto do pedido (repetir a chamada da o mesmo resultado)
    @PutMapping("/{id}/desconto")
    public Pedido definirDesconto(@PathVariable String id, @Valid @RequestBody DescontoDTO dados) {
        return pedidoService.aplicarDesconto(id, dados.percentual());
    }

    @PostMapping("/{id}/cancelamento")
    public Pedido cancelar(@PathVariable String id, @Valid @RequestBody CancelamentoDTO dados) {
        return pedidoService.cancelar(id, dados.motivo());
    }
}
