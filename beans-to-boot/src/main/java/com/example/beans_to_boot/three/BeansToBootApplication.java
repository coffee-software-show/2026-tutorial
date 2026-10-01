package com.example.beans_to_boot.three;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Set;

// AOP with jdk/cglib proxies

public class BeansToBootApplication {

    public static void main(String[] args) {
        var db = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/mydatabase", "myuser", "secret");
        var jdbc = JdbcClient.create(db);
        var txManager = new DataSourceTransactionManager(db);
        var transactionTemplate = new TransactionTemplate(txManager);
        var animals = new DefaultAnimalRepository3(jdbc);
        var txAnimals = (AnimalRepository) Transactions.proxy(animals, transactionTemplate);
        test(txAnimals);
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

class Transactions {

    private static Object jdkProxy(Object target, TransactionTemplate tt) {
        return Proxy.newProxyInstance(target.getClass().getClassLoader(),
                target.getClass().getInterfaces(), (_, method, args) ->
                        delegate(tt, target, method, args));
    }

    static Object proxy(Object target, TransactionTemplate tt) {
        var pfb = new ProxyFactoryBean();
        pfb.setTarget(target);
        pfb.setProxyTargetClass(true);
        for (var c : target.getClass().getInterfaces())
            pfb.addInterface(c);
        pfb.addAdvice((MethodInterceptor) invocation ->
                delegate(tt, target, invocation.getMethod(), invocation.getArguments()));
        return pfb.getObject();
    }

    private static Object delegate(
            TransactionTemplate transactionTemplate,//
            Object target, //
            Method m,//
            Object[] parms//
    ) {
        return transactionTemplate.execute(_ -> {
            try {
                IO.println("before the tx");
                var res = m.invoke(target, parms);
                IO.println("after the tx");
                return res;
            }//
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
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

