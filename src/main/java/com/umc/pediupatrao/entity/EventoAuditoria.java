package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Evento da trilha de auditoria (RF05). Imutavel: so tem construtor e getters.
 */
@Document(collection = "trilha_auditoria")
@CompoundIndex(name = "por_registro", def = "{'entidade': 1, 'registroId': 1, 'momento': -1}")
public class EventoAuditoria {

    @Id
    private String id;

    @Indexed
    private LocalDateTime momento;

    private String usuario;
    private Perfil perfil;
    private TipoEvento tipo;
    private String entidade;
    private String registroId;
    private List<Mudanca> mudancas = new ArrayList<>();
    private String justificativa;

    protected EventoAuditoria() {
        // usado pelo Spring Data ao ler do MongoDB
    }

    public EventoAuditoria(LocalDateTime momento, String usuario, Perfil perfil, TipoEvento tipo,
            String entidade, String registroId, List<Mudanca> mudancas, String justificativa) {
        this.momento = momento;
        this.usuario = usuario;
        this.perfil = perfil;
        this.tipo = tipo;
        this.entidade = entidade;
        this.registroId = registroId;
        this.mudancas = new ArrayList<>(mudancas);
        this.justificativa = justificativa;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getMomento() {
        return momento;
    }

    public String getUsuario() {
        return usuario;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public String getEntidade() {
        return entidade;
    }

    public String getRegistroId() {
        return registroId;
    }

    public List<Mudanca> getMudancas() {
        return List.copyOf(mudancas);
    }

    public String getJustificativa() {
        return justificativa;
    }
}
