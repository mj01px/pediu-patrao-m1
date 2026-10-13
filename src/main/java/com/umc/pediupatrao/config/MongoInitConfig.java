package com.umc.pediupatrao.config;

import java.util.stream.Stream;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoInitConfig implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public MongoInitConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        Stream.of("pedidos", "clientes", "usuarios", "produtos", "horariosAtendimento", "trilha_auditoria")
                .filter(colecao -> !mongoTemplate.collectionExists(colecao))
                .forEach(mongoTemplate::createCollection);
    }
}
