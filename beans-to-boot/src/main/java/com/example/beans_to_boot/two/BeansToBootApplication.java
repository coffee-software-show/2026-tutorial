package com.example.beans_to_boot.two;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;

interface AnimalRepository {

    Collection<Animal> findAll();

    Animal save(String name, String description, Animal.Type type);

    void deleteAll();
}

// "good" OOP
public class BeansToBootApplication {

    /*
    public static void main(String[] args) {
        var db = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/mydatabase", "myuser", "secret");
        var jdbc = JdbcClient.create(db);
        var txManager = new DataSourceTransactionManager(db);
        var transactionTemplate = new TransactionTemplate(txManager);
        var animals = new DefaultAnimalRepository3(jdbc);
        var txAnimals = new TransactionalAnimalRepository(transactionTemplate, animals);
        test(txAnimals);
    } */

    static void test(AnimalRepository repository) {
        repository.deleteAll();
        var fido = repository.save("Fido", "A friendly dog", Animal.Type.DOG);
        var rex = repository.save("Rex", "A friendly dog", Animal.Type.DOG);
        var garfield = repository.save("Garfield", "A friendly dog", Animal.Type.CAT);
        IO.println(fido);
        IO.println(garfield);
        IO.println(rex);
        IO.println("===================================");
        repository.findAll().forEach(IO::println);
    }
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

class TransactionalAnimalRepository implements AnimalRepository {

    private final TransactionTemplate transactionTemplate;

    private final AnimalRepository repository;

    TransactionalAnimalRepository(TransactionTemplate transactionTemplate, AnimalRepository repository) {
        this.transactionTemplate = transactionTemplate;
        this.repository = repository;
    }

    @Override
    public Collection<Animal> findAll() {
        return this.transactionTemplate.execute(status -> this.repository.findAll());
    }

    @Override
    public Animal save(String name, String description, Animal.Type type) {
        return this.transactionTemplate.execute(s -> this.repository.save(name, description, type));
    }

    @Override
    public void deleteAll() {
        this.transactionTemplate.executeWithoutResult(s -> this.repository.deleteAll());
    }
}

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
                .sql("select * from animal ")
                .params()
                .query(this.animalRowMapper)
                .list();
    }

    public Animal findById(int id) {
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

