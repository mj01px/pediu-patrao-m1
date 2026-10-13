package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.erro.NegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.seguranca.RegrasAcesso;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ClienteService {

    private static final String ENTIDADE = "Cliente";

    // Campos auditados do cliente, declarados uma vez so. Telefone e dado pessoal: vai mascarado.
    private static final ComparadorCampos<Cliente> CAMPOS_AUDITADOS = ComparadorCampos.para(Cliente.class)
            .campo("nome", Cliente::getNome)
            .sensivel("telefone", Cliente::getTelefone)
            .campo("cep", Cliente::getCep)
            .campo("logradouro", Cliente::getLogradouro)
            .campo("numero", Cliente::getNumero)
            .campo("complemento", Cliente::getComplemento)
            .campo("bairro", Cliente::getBairro)
            .campo("cidade", Cliente::getCidade)
            .campo("estado", Cliente::getEstado);

    private final ClienteRepository clienteRepository;
    private final ApplicationEventPublisher eventos;

    public ClienteService(ClienteRepository clienteRepository, ApplicationEventPublisher eventos) {
        this.clienteRepository = clienteRepository;
        this.eventos = eventos;
    }

    @PreAuthorize(RegrasAcesso.OPERACAO)
    public List<Cliente> listar() {
        return clienteRepository.findAll(Sort.by("nome"));
    }

    @PreAuthorize(RegrasAcesso.OPERACAO)
    public Optional<Cliente> buscar(String id) {
        return clienteRepository.findById(id);
    }

    @PreAuthorize(RegrasAcesso.SO_ATENDENTE)
    public Cliente cadastrar(Cliente dados) {
        validar(dados);
        Cliente novo = new Cliente(); // objeto novo: ignora qualquer id vindo da requisicao
        novo.atualizarCom(dados);
        Cliente salvo = clienteRepository.save(novo);
        eventos.publishEvent(RegistroAlteracao.de(TipoEvento.CLIENTE_CADASTRADO, ENTIDADE, salvo.getId(),
                CAMPOS_AUDITADOS.comparar(null, salvo)));
        return salvo;
    }

    @PreAuthorize(RegrasAcesso.SO_ATENDENTE)
    public Cliente alterar(String id, Cliente dados) {
        validar(dados);
        Cliente atual = clienteRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));

        // Compara ANTES de aplicar: e assim que a trilha sabe o valor anterior
        Mudancas mudancas = CAMPOS_AUDITADOS.comparar(atual, dados);
        atual.atualizarCom(dados);
        Cliente salvo = clienteRepository.save(atual);

        if (!mudancas.vazia()) {
            eventos.publishEvent(RegistroAlteracao.de(TipoEvento.CLIENTE_ALTERADO, ENTIDADE, id, mudancas));
        }
        return salvo;
    }

    /** Exclusao fica com o GERENTE por ser irreversivel. */
    @PreAuthorize(RegrasAcesso.SO_GERENTE)
    public void remover(String id) {
        Cliente atual = clienteRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));
        clienteRepository.deleteById(id);
        // Na exclusao guarda so o nome (minimo necessario para saber quem saiu)
        eventos.publishEvent(RegistroAlteracao.de(TipoEvento.CLIENTE_REMOVIDO, ENTIDADE, id,
                Mudancas.nova().registrar("nome", atual.getNome(), null)));
    }

    private static void validar(Cliente cliente) {
        if (!StringUtils.hasText(cliente.getNome())) {
            throw new NegocioException("O nome do cliente é obrigatório.");
        }
        if (!StringUtils.hasText(cliente.getTelefone())) {
            throw new NegocioException("O telefone do cliente é obrigatório.");
        }
    }
}
