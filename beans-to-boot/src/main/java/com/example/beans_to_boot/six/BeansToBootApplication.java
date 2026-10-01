package com.example.beans_to_boot.six;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

// Spring Boot
@SpringBootApplication
@ImportRuntimeHints(BeansToBootApplication.Hints.class)
public class BeansToBootApplication {

    @EventListener
    void after(ContextRefreshedEvent contextRefreshedEvent) {
        IO.println("application context refreshed " + contextRefreshedEvent);
    }

    @Bean
    ApplicationRunner runner(AnimalRepository repository) {
        return _ -> test(repository);
    }

    public static void main(String[] args) {
        SpringApplication.run(BeansToBootApplication.class, args);
    }

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
            hints.reflection().registerType(TypeReference.of("com.google.protobuf.ExtensionRegistry"),
                    MemberCategory.values());
        }
    }

    @Bean
    JdbcPostgresDialect jdbcPostgresDialect() {
        return JdbcPostgresDialect.INSTANCE;
    }

    static void test(AnimalRepository repository) {

        repository.deleteAll();

        var fido = repository.save("Fido", "A friendly dog", Animal.Type.DOG);
        var rex = repository.save("Rex", "A friendly dog", Animal.Type.DOG);
        var garfield = repository.save("Garfield", "A friendly dog", Animal.Type.CAT);

        for (var a : Set.of(fido, rex, garfield))
            IO.println(a);

        IO.println("===================================");
        repository.findAll().forEach(IO::println);
    }


}

interface AnimalRepository {

    Collection<Animal> findAll();

    Animal save(String name, String description, Animal.Type type);

    void deleteAll();
}

record Animal(int id, String name, String description, Type type) {

    enum Type {
        DOG,
        CAT,
        BIRD,
        FISH,
        REPTILE,
        OTHER
    }
}

@Repository
@Transactional
class DefaultAnimalRepository3 implements AnimalRepository {

    private final JdbcClient jdbcClient;

    private final RowMapper<Animal> animalRowMapper = (rs, _) -> new Animal(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            Animal.Type.valueOf(rs.getString("type")));

    DefaultAnimalRepository3(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Collection<Animal> findAll() {
        return this.jdbcClient
                .sql("select * from animal order by id")
                .params()
                .query(this.animalRowMapper)
                .list();
    }

    private Animal findById(int id) {
        return this.jdbcClient
                .sql("select * from animal where id =? ")
                .params(id)
                .query(this.animalRowMapper)
                .list().iterator().next();
    }

    @Override
    public Animal save(String name, String description, Animal.Type type) {
        var gkh = new GeneratedKeyHolder();
        this.jdbcClient
                .sql("INSERT INTO animal (name, description, type) VALUES (?, ?, ?)")
                .params(name, description, type.name())
                .update(gkh);
        return this.findById(((Number) gkh.getKeys().get("id")).intValue());

    }

    @Override
    public void deleteAll() {
        this.jdbcClient.sql("delete from animal").update();
    }
}

