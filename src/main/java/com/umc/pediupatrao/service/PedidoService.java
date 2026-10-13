package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.NovoPedidoDTO;
import com.umc.pediupatrao.entity.Carimbo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.erro.NegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.seguranca.RegrasAcesso;
import com.umc.pediupatrao.seguranca.UsuarioLogado;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Orquestra os pedidos: carrega, pede para o Pedido aplicar a regra, salva e avisa a auditoria.
 * As regras em si (fluxo, limite de desconto, motivo de cancelamento) moram na entidade Pedido.
 */
@Service
public class PedidoService {

    private static final String ENTIDADE = "Pedido";

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioLogado usuarioLogado;
    private final ApplicationEventPublisher eventos;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository, UsuarioLogado usuarioLogado, ApplicationEventPublisher eventos) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioLogado = usuarioLogado;
        this.eventos = eventos;
    }

    @PreAuthorize(RegrasAcesso.QUALQUER_PERFIL)
    public List<Pedido> listar() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "entrada.momento"));
    }

    @PreAuthorize(RegrasAcesso.QUALQUER_PERFIL)
    public Pedido buscar(String id) {
        return carregar(id);
    }

    @PreAuthorize(RegrasAcesso.OPERACAO)
    public Pedido registrar(NovoPedidoDTO dados) {
        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new NaoEncontradoException("Cliente", dados.clienteId()));
        List<ItemPedido> itens = dados.itens().stream().map(this::montarItem).toList();

        Pedido pedido = pedidoRepository.save(
                Pedido.abrir(cliente, itens, dados.observacao(), usuarioLogado.carimbo()));

        publicar(TipoEvento.PEDIDO_REGISTRADO, pedido, Mudancas.nova()
                .registrar("cliente", null, pedido.getNomeCliente())
                .registrar("itens", null, descreverItens(pedido))
                .registrar("total", null, pedido.getTotal())
                .registrar("status", null, pedido.getStatus()), null);
        return pedido;
    }

    @PreAuthorize(RegrasAcesso.OPERACAO)
    public Pedido avancar(String id, StatusPedido destino) {
        if (destino == null) {
            throw new NegocioException("Informe para qual etapa o pedido vai.");
        }
        Pedido pedido = carregar(id);
        StatusPedido anterior = pedido.getStatus();
        pedido.avancarPara(destino, usuarioLogado.carimbo());
        pedido = pedidoRepository.save(pedido);

        Mudancas mudancas = Mudancas.nova().registrar("status", anterior, pedido.getStatus());
        if (destino.marcaSaida()) {
            Carimbo saida = pedido.getSaida();
            mudancas.registrar("saidaPor", null, saida.getUsuario()).registrar("saidaEm", null, saida.getMomento());
        }
        publicar(TipoEvento.PEDIDO_AVANCOU, pedido, mudancas, null);
        return pedido;
    }

    @PreAuthorize(RegrasAcesso.SO_GERENTE)
    public Pedido cancelar(String id, String motivo) {
        Pedido pedido = carregar(id);
        StatusPedido anterior = pedido.getStatus();
        pedido.cancelar(motivo, usuarioLogado.carimbo());
        pedido = pedidoRepository.save(pedido);

        publicar(TipoEvento.PEDIDO_CANCELADO, pedido,
                Mudancas.nova().registrar("status", anterior, pedido.getStatus()),
                pedido.getMotivoCancelamento());
        return pedido;
    }

    @PreAuthorize(RegrasAcesso.SO_GERENTE)
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        Pedido pedido = carregar(id);
        BigDecimal percentualAntes = pedido.getPercentualDesconto();
        BigDecimal descontoAntes = pedido.getDesconto();
        BigDecimal totalAntes = pedido.getTotal();

        pedido.aplicarDesconto(percentual, usuarioLogado.carimbo());
        pedido = pedidoRepository.save(pedido);

        publicar(TipoEvento.PEDIDO_COM_DESCONTO, pedido, Mudancas.nova()
                .registrar("percentualDesconto", percentualAntes, pedido.getPercentualDesconto())
                .registrar("desconto", descontoAntes, pedido.getDesconto())
                .registrar("total", totalAntes, pedido.getTotal()), null);
        return pedido;
    }

    // O preco SEMPRE vem do cardapio, nunca da requisicao
    private ItemPedido montarItem(NovoPedidoDTO.ItemDTO item) {
        if (item.quantidade() < 1 || item.quantidade() > 99) {
            throw new NegocioException("Quantidade fora do permitido (1 a 99).");
        }
        Produto produto = produtoRepository.findById(item.produtoId())
                .orElseThrow(() -> new NaoEncontradoException("Produto", item.produtoId()));
        if (!produto.disponivelParaVenda()) {
            throw new NegocioException(produto.getNome() + " não está disponível para venda.");
        }
        return new ItemPedido(produto, item.quantidade());
    }

    private Pedido carregar(String id) {
        return pedidoRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Pedido", id));
    }

    private void publicar(TipoEvento tipo, Pedido pedido, Mudancas mudancas, String justificativa) {
        eventos.publishEvent(RegistroAlteracao.de(tipo, ENTIDADE, pedido.getId(), mudancas)
                .comJustificativa(justificativa));
    }

    private static String descreverItens(Pedido pedido) {
        return pedido.getItens().stream().map(ItemPedido::toString).collect(Collectors.joining(", "));
    }
}
