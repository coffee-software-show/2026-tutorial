package com.example.beans_to_boot.five;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

// Spring Boot
@SpringBootApplication
public class BeansToBootApplication {

    @EventListener
    void after(ContextRefreshedEvent contextRefreshedEvent) {
        IO.println("application context refreshed " + contextRefreshedEvent);
    }

    @Bean
    ApplicationRunner runner (AnimalRepository repository) {
        return _ -> test(repository);
    }

    public static void main(String[] args) {
         SpringApplication.run(BeansToBootApplication.class,args);
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

