package com.umc.pediupatrao.entity;

import com.umc.pediupatrao.erro.NegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Pedido com as proprias regras (RF02, RF03 e RF04).
 *
 * Nao ha setters de status nem de valores: a unica forma de mudar o pedido e
 * pelos metodos abrir, avancarPara, cancelar e aplicarDesconto, que validam
 * a regra antes de alterar qualquer coisa.
 */
@Document(collection = "pedidos")
public class Pedido {

    public static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20.00");
    private static final int TAMANHO_MINIMO_MOTIVO = 10;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    @Id
    private String id;

    // Se duas pessoas alterarem o mesmo pedido ao mesmo tempo, a segunda gravacao falha
    @Version
    private Long versao;

    private String clienteId;
    private String nomeCliente;
    private List<ItemPedido> itens = new ArrayList<>();
    private String observacao;
    private StatusPedido status;

    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal percentualDesconto = BigDecimal.ZERO;
    private BigDecimal desconto = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;

    private Carimbo entrada;
    private Carimbo saida;
    private Carimbo carimboDesconto;
    private Carimbo cancelamento;
    private String motivoCancelamento;

    private List<MarcoPedido> linhaDoTempo = new ArrayList<>();

    protected Pedido() {
        // usado pelo Spring Data ao ler do MongoDB
    }

    /** RF02 + RF03 passo 1: cria o pedido em RECEBIDO, com entrada carimbada. */
    public static Pedido abrir(Cliente cliente, List<ItemPedido> itens, String observacao, Carimbo entrada) {
        if (itens == null || itens.isEmpty()) {
            throw new NegocioException("O pedido precisa de pelo menos um item.");
        }
        Pedido pedido = new Pedido();
        pedido.clienteId = cliente.getId();
        pedido.nomeCliente = cliente.getNome();
        pedido.itens = new ArrayList<>(itens);
        pedido.observacao = observacao == null || observacao.isBlank() ? null : observacao.trim();
        pedido.entrada = entrada;
        pedido.registrarEtapa(StatusPedido.RECEBIDO, entrada);
        pedido.recalcular();
        return pedido;
    }

    /** RF03: leva o pedido para a proxima etapa, se o fluxo permitir. */
    public void avancarPara(StatusPedido destino, Carimbo carimbo) {
        exigirNaoCancelado();
        if (destino == StatusPedido.CANCELADO) {
            throw new NegocioException("Cancelamento tem operação própria e é exclusivo do gerente.");
        }
        if (!status.permite(destino)) {
            throw new NegocioException("Não é possível ir de " + status.getRotulo() + " para "
                    + destino.getRotulo() + ". Próximas etapas: " + status.seguintes() + ".");
        }
        if (destino.marcaSaida()) {
            this.saida = carimbo;
        }
        registrarEtapa(destino, carimbo);
    }

    /** RF04: cancelamento com motivo, so antes da saida. Itens e valores ficam preservados. */
    public void cancelar(String motivo, Carimbo carimbo) {
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.length() < TAMANHO_MINIMO_MOTIVO) {
            throw new NegocioException("Informe o motivo do cancelamento (mínimo de "
                    + TAMANHO_MINIMO_MOTIVO + " caracteres).");
        }
        exigirNaoCancelado();
        if (!status.emAberto()) {
            throw new NegocioException("O pedido já saiu (" + status.getRotulo() + ") e não pode ser cancelado.");
        }
        this.cancelamento = carimbo;
        this.motivoCancelamento = texto;
        registrarEtapa(StatusPedido.CANCELADO, carimbo);
    }

    /** RF04: desconto percentual de ate 20%, so antes da saida. Substitui um desconto anterior. */
    public void aplicarDesconto(BigDecimal percentual, Carimbo carimbo) {
        if (percentual == null) {
            throw new NegocioException("Informe o percentual de desconto.");
        }
        BigDecimal valor = percentual.setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() <= 0 || valor.compareTo(DESCONTO_MAXIMO) > 0) {
            throw new NegocioException("O desconto deve ser maior que 0% e no máximo "
                    + DESCONTO_MAXIMO.toPlainString() + "%.");
        }
        exigirNaoCancelado();
        if (!status.emAberto()) {
            throw new NegocioException("Desconto só pode ser dado antes da saída do pedido.");
        }
        this.percentualDesconto = valor;
        this.carimboDesconto = carimbo;
        recalcular();
    }

    private void exigirNaoCancelado() {
        if (status == StatusPedido.CANCELADO) {
            throw new NegocioException("Pedido cancelado não aceita mais alterações.");
        }
    }

    private void registrarEtapa(StatusPedido novoStatus, Carimbo carimbo) {
        this.status = novoStatus;
        this.linhaDoTempo.add(new MarcoPedido(novoStatus, carimbo));
    }

    // O total so e calculado aqui, a partir dos itens: nenhuma requisicao define valores
    private void recalcular() {
        subtotal = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        desconto = subtotal.multiply(percentualDesconto).divide(CEM, 2, RoundingMode.HALF_UP);
        total = subtotal.subtract(desconto);
    }

    public String getId() {
        return id;
    }

    public Long getVersao() {
        return versao;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public List<ItemPedido> getItens() {
        return List.copyOf(itens);
    }

    public String getObservacao() {
        return observacao;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Carimbo getEntrada() {
        return entrada;
    }

    public Carimbo getSaida() {
        return saida;
    }

    public Carimbo getCarimboDesconto() {
        return carimboDesconto;
    }

    public Carimbo getCancelamento() {
        return cancelamento;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public List<MarcoPedido> getLinhaDoTempo() {
        return List.copyOf(linhaDoTempo);
    }
}
