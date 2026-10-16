package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.UsuarioFormDTO;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.erro.NegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.seguranca.EncerradorDeSessoes;
import com.umc.pediupatrao.seguranca.RegrasAcesso;
import com.umc.pediupatrao.seguranca.UsuarioLogado;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Administracao de usuarios. Toda a classe e exclusiva do ADMIN. */
@Service
@PreAuthorize(RegrasAcesso.SO_ADMIN)
public class UsuarioService {

    private static final String ENTIDADE = "Usuario";
    private static final int SENHA_MINIMA = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogado usuarioLogado;
    private final ApplicationEventPublisher eventos;
    private final EncerradorDeSessoes encerradorDeSessoes;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            UsuarioLogado usuarioLogado, ApplicationEventPublisher eventos, EncerradorDeSessoes encerradorDeSessoes) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogado = usuarioLogado;
        this.eventos = eventos;
        this.encerradorDeSessoes = encerradorDeSessoes;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll(Sort.by("perfil", "username"));
    }

    public Optional<Usuario> buscar(String id) {
        return usuarioRepository.findById(id);
    }

    public Usuario criar(UsuarioFormDTO form) {
        String login = loginNormalizado(form);
        exigirPerfil(form.getPerfil());
        if (usuarioRepository.existsByUsername(login)) {
            throw new NegocioException("O login " + login + " já está em uso.");
        }
        if (!form.informouSenha()) {
            throw new NegocioException("Defina uma senha para o novo usuário.");
        }
        exigirSenhaForte(form.getSenha());

        Usuario novo = new Usuario();
        novo.setUsername(login);
        novo.setPerfil(form.getPerfil());
        novo.setPassword(passwordEncoder.encode(form.getSenha()));
        Usuario salvo = usuarioRepository.save(novo);

        eventos.publishEvent(RegistroAlteracao.de(TipoEvento.USUARIO_CRIADO, ENTIDADE, salvo.getId(),
                Mudancas.nova().registrar("username", null, login).registrar("perfil", null, salvo.getPerfil())));
        return salvo;
    }

    public Usuario alterar(String id, UsuarioFormDTO form) {
        Usuario alvo = usuarioRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário", id));
        String login = loginNormalizado(form);
        exigirPerfil(form.getPerfil());
        if (usuarioRepository.existsByUsernameAndIdNot(login, id)) {
            throw new NegocioException("O login " + login + " já está em uso.");
        }
        protegerContraAutoalteracao(alvo, form.getPerfil());

        String loginAnterior = alvo.getUsername();
        boolean mudouAcesso = alvo.getPerfil() != form.getPerfil() || !loginAnterior.equals(login) || form.informouSenha();
        Mudancas mudancas = Mudancas.nova()
                .registrar("username", loginAnterior, login)
                .registrar("perfil", alvo.getPerfil(), form.getPerfil());
        alvo.setUsername(login);
        alvo.setPerfil(form.getPerfil());

        if (form.informouSenha()) {
            exigirSenhaForte(form.getSenha());
            alvo.setPassword(passwordEncoder.encode(form.getSenha()));
            // A trilha registra QUE a senha mudou, nunca o valor
            mudancas.registrar("senha", "(sigilo)", "(redefinida)");
        }

        Usuario salvo = usuarioRepository.save(alvo);
        if (!mudancas.vazia()) {
            eventos.publishEvent(RegistroAlteracao.de(TipoEvento.USUARIO_ALTERADO, ENTIDADE, id, mudancas));
        }
        // Perfil, login ou senha novos so valem num login novo: quem estava logado sai na hora
        if (mudouAcesso) {
            encerradorDeSessoes.encerrar(loginAnterior);
        }
        return salvo;
    }

    public void remover(String id) {
        Usuario alvo = usuarioRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário", id));
        if (ehOProprio(alvo)) {
            throw new NegocioException("Você não pode remover o seu próprio usuário.");
        }
        if (alvo.getPerfil() == Perfil.ADMIN) {
            garantirOutroAdmin();
        }
        usuarioRepository.deleteById(id);
        encerradorDeSessoes.encerrar(alvo.getUsername());
        eventos.publishEvent(RegistroAlteracao.de(TipoEvento.USUARIO_REMOVIDO, ENTIDADE, id,
                Mudancas.nova().registrar("username", alvo.getUsername(), null).registrar("perfil", alvo.getPerfil(), null)));
    }

    /** Ninguem muda o proprio perfil (nem para cima, nem para baixo) e sempre sobra um ADMIN. */
    private void protegerContraAutoalteracao(Usuario alvo, Perfil novoPerfil) {
        if (alvo.getPerfil() == novoPerfil) {
            return;
        }
        if (ehOProprio(alvo)) {
            throw new NegocioException("Você não pode alterar o seu próprio perfil.");
        }
        if (alvo.getPerfil() == Perfil.ADMIN) {
            garantirOutroAdmin();
        }
    }

    private boolean ehOProprio(Usuario alvo) {
        return alvo.getUsername().equals(usuarioLogado.operador().login());
    }

    private void garantirOutroAdmin() {
        if (usuarioRepository.countByPerfil(Perfil.ADMIN) < 2) {
            throw new NegocioException("O sistema precisa de pelo menos um ADMIN.");
        }
    }

    private static void exigirPerfil(Perfil perfil) {
        if (perfil == null) {
            throw new NegocioException("Escolha um perfil: ADMIN, GERENTE ou ATENDENTE.");
        }
    }

    private static void exigirSenhaForte(String senha) {
        if (senha.length() < SENHA_MINIMA) {
            throw new NegocioException("A senha precisa de pelo menos " + SENHA_MINIMA + " caracteres.");
        }
    }

    private static String loginNormalizado(UsuarioFormDTO form) {
        if (form.getUsername() == null || form.getUsername().isBlank()) {
            throw new NegocioException("Informe o login.");
        }
        return form.getUsername().trim().toLowerCase();
    }
}
