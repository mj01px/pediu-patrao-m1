package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.EventoAuditoria;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Repositorio "somente inclusao". Estende Repository (e nao MongoRepository),
 * entao so existem os metodos abaixo: nenhum delete, nenhum update.
 */
public interface EventoAuditoriaRepository extends Repository<EventoAuditoria, String> {

    <S extends EventoAuditoria> S save(S evento);

    List<EventoAuditoria> findTop300ByOrderByMomentoDesc();

    List<EventoAuditoria> findTop300ByEntidadeOrderByMomentoDesc(String entidade);

    List<EventoAuditoria> findByEntidadeAndRegistroIdOrderByMomentoDesc(String entidade, String registroId);
}
