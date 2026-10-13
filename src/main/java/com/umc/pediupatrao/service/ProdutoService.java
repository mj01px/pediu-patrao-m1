package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.erro.NegocioException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.seguranca.RegrasAcesso;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @PreAuthorize(RegrasAcesso.QUALQUER_PERFIL)
    public List<Produto> listar() {
        return produtoRepository.findAll(Sort.by("tipo", "nome"));
    }

    @PreAuthorize(RegrasAcesso.QUALQUER_PERFIL)
    public List<Produto> listarDisponiveis() {
        return listar().stream().filter(Produto::disponivelParaVenda).toList();
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public Produto criar(Produto produto) {
        produto.setId(null); // criar nunca sobrescreve um produto existente
        return produtoRepository.save(validado(produto));
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public Produto alterar(String id, Produto produto) {
        if (!produtoRepository.existsById(id)) {
            throw new NaoEncontradoException("Produto", id);
        }
        produto.setId(id);
        return produtoRepository.save(validado(produto));
    }

    // Pedidos guardam nome e preco de cada item, entao remover o produto nao apaga historico
    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public void remover(String id) {
        produtoRepository.deleteById(id);
    }

    private static Produto validado(Produto produto) {
        if (!StringUtils.hasText(produto.getNome())) {
            throw new NegocioException("Informe o nome do produto.");
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new NegocioException("O preço precisa ser maior que zero.");
        }
        if (produto.getAtivo() == null) {
            produto.setAtivo(Boolean.FALSE);
        }
        return produto;
    }
}
