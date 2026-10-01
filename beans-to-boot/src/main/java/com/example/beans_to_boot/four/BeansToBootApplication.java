package com.example.beans_to_boot.four;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Set;

interface AnimalRepository {

    Collection<Animal> findAll();

    Animal save(String name, String description, Animal.Type type);

    void deleteAll();
}

// Spring Framework
@ComponentScan
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
@Configuration
class MyConfiguration {

    @Bean
    DriverManagerDataSource driverManagerDataSource(Environment environment) {
        return new DriverManagerDataSource(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"));
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(DataSource driverManagerDataSource) {
        return new DataSourceTransactionManager(driverManagerDataSource);
    }

    @Bean
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    @EventListener
    void after(ContextRefreshedEvent contextRefreshedEvent) {
        IO.println("application context refreshed " + contextRefreshedEvent);
    }

}

public class BeansToBootApplication {

   /* public static void main(String[] args) {
        var ac = new AnnotationConfigApplicationContext(MyConfiguration.class);
        var txAnimals = ac.getBean(AnimalRepository.class);
        test(txAnimals);
    }*/

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

