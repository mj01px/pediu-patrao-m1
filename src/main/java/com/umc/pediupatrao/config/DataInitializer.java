package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Carga inicial: um usuario por perfil e um cardapio basico, so se ainda nao existirem. */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final Map<String, Perfil> EQUIPE = Map.of(
            "admin", Perfil.ADMIN,
            "gerente", Perfil.GERENTE,
            "atendente", Perfil.ATENDENTE);

    private final UsuarioRepository usuarios;
    private final ProdutoRepository produtos;
    private final PasswordEncoder passwordEncoder;
    private final String senhaInicial;

    public DataInitializer(UsuarioRepository usuarios, ProdutoRepository produtos, PasswordEncoder passwordEncoder,
            @Value("${pediupatrao.senha-inicial}") String senhaInicial) {
        this.usuarios = usuarios;
        this.produtos = produtos;
        this.passwordEncoder = passwordEncoder;
        this.senhaInicial = senhaInicial;
    }

    @Override
    public void run(ApplicationArguments args) {
        EQUIPE.forEach(this::garantirUsuario);
        if (produtos.count() == 0) {
            produtos.save(produto("Pizza Calabresa", "Pizza", 49.90));
            produtos.save(produto("Pizza Quatro Queijos", "Pizza", 58.00));
            produtos.save(produto("Pizza Frango com Catupiry", "Pizza", 55.50));
            produtos.save(produto("Esfiha de Carne", "Esfiha", 7.00));
            produtos.save(produto("Guaraná 2L", "Bebida", 13.00));
            log.info("Cardápio inicial cadastrado.");
        }
    }

    private void garantirUsuario(String login, Perfil perfil) {
        Usuario usuario = usuarios.findByUsername(login).orElse(null);
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setUsername(login);
            usuario.setPassword(passwordEncoder.encode(senhaInicial));
            usuario.setPerfil(perfil);
            usuarios.save(usuario);
            log.info("Usuário inicial criado: {} ({})", login, perfil); // a senha nunca vai para o log
        } else if (usuario.getPerfil() == null) {
            // usuario da versao antiga (sem perfil) recebe o perfil padrao
            usuario.setPerfil(perfil);
            usuarios.save(usuario);
        }
    }

    private static Produto produto(String nome, String tipo, double preco) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }
}
