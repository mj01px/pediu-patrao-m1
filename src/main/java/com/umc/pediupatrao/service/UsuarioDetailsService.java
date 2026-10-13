package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        // Antes todo mundo recebia ROLE_USER. Agora a role e o perfil gravado no banco.
        boolean semPerfil = usuario.getPerfil() == null;
        List<SimpleGrantedAuthority> roles = semPerfil
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(roles)
                .disabled(semPerfil) // sem perfil, sem login
                .build();
    }
}
