package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByClienteId(String clienteId);

    /** Pedidos que entraram em [desde, ate): base de todos os numeros do dashboard. Usa o indice de entrada.momento. */
    @Query("{ 'entrada.momento': { $gte: ?0, $lt: ?1 } }")
    List<Pedido> findPorEntradaNoPeriodo(LocalDateTime desde, LocalDateTime ate);
}
