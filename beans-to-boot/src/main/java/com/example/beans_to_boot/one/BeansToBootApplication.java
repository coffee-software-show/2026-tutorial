package com.example.beans_to_boot.one;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;

// mistakes were made.
public class BeansToBootApplication {

    /*
    public static void main(String[] args) {
        var db = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/mydatabase", "myuser", "secret");
        var jdbc = JdbcClient.create(db);
        var animals = new DefaultAnimalRepository3(jdbc);
        test(animals);
    }
    */

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


class DefaultAnimalRepository3 implements AnimalRepository {

    private final JdbcClient dataSource;

    private final RowMapper<Animal> animalRowMapper = (rs, _) -> new Animal(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            Animal.Type.valueOf(rs.getString("type")));

    DefaultAnimalRepository3(JdbcClient dataSource) {
        this.dataSource = dataSource;
    }

    private Collection<Animal> select(String sql, Object... parms) {
        return this.dataSource
                .sql(sql)
                .params(parms)
                .query(this.animalRowMapper)
                .list();
    }

    @Override
    public Collection<Animal> findAll() {
        return this.select("select * from animal ");
    }

    public Animal findById(int id) {
        return this.select("select * from animal where id =? ", id).iterator().next();
    }

    @Override
    public Animal save(String name, String description, Animal.Type type) {
        var gkh = new GeneratedKeyHolder();
        this.dataSource
                .sql("INSERT INTO animal (name, description, type) VALUES (?, ?, ?)")
                .params(name, description, type.name())
                .update(gkh);
        return this.findById(((Number)gkh.getKeys().get ("id")).intValue() );

    }

    @Override
    public void deleteAll() {
        this.dataSource.sql("delete from animal").update();
    }

}

class DefaultAnimalRepository2 implements AnimalRepository {

    private final DataSource dataSource;

    DefaultAnimalRepository2(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Collection<Animal> select(String sql) {
        var all = new ArrayList<Animal>();
        try (
                var c = this.dataSource.getConnection();
                var p = c.prepareStatement(sql);
                var r = p.executeQuery()
        ) {
            while (r.next())
                all.add(new Animal(r.getInt("id"),
                        r.getString("name"),
                        r.getString("description"),
                        Animal.Type.valueOf(r.getString("type"))));
        }//
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return all;
    }

    @Override
    public Collection<Animal> findAll() {
        return this.select("select * from animal ");
    }

    public Animal findById(int id) {
        return this.select("select * from animal where id = " + id).iterator().next();
    }

    @Override
    public Animal save(String name, String description, Animal.Type type) {

        try (var c = this.dataSource.getConnection();
             var p = c.prepareStatement("INSERT INTO animal (name, description, type) VALUES (?, ?, ?)",
                     new String[]{"id"});) {
            p.setString(1, name);
            p.setString(2, description);
            p.setString(3, type.name());
            if (p.executeUpdate() > 0) {
                try (var generatedKeys = p.getGeneratedKeys();) {
                    if (generatedKeys.next()) {
                        var id = generatedKeys.getInt(1);
                        return this.findById(id);
                    }
                }
            }
        }//
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public void deleteAll() {
        try (var c = this.dataSource.getConnection();
             var p = c.prepareStatement("delete from animal");) {
            p.executeUpdate();
        }//
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

class DefaultAnimalRepository1 implements AnimalRepository {

    private final DataSource dataSource = new DriverManagerDataSource(
            "jdbc:postgresql://localhost:5432/mydatabase", "myuser", "secret");

    @Override
    public void deleteAll() {
        try (var c = this.dataSource.getConnection();
             var p = c.prepareStatement("delete from animal");) {
            p.executeUpdate();
        }//
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<Animal> select(String sql) {
        var all = new ArrayList<Animal>();
        try (
                var c = this.dataSource.getConnection();
                var p = c.prepareStatement(sql);
                var r = p.executeQuery()
        ) {
            while (r.next())
                all.add(new Animal(r.getInt("id"),
                        r.getString("name"),
                        r.getString("description"), Animal.Type.valueOf(r.getString("type"))));
        }// 
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return all;
    }

    @Override
    public Collection<Animal> findAll() {
        return this.select("select * from animal ");
    }

    public Animal findById(int id) {
        return this.select("select * from animal where id = " + id).iterator().next();
    }

    @Override
    public Animal save(String name, String description, Animal.Type type) {

        try (var c = this.dataSource.getConnection();
             var p = c.prepareStatement("INSERT INTO animal (name, description, type) VALUES (?, ?, ?)",
                     new String[]{"id"});) {
            p.setString(1, name);
            p.setString(2, description);
            p.setString(3, type.name());
            if (p.executeUpdate() > 0) {
                try (var generatedKeys = p.getGeneratedKeys();) {
                    if (generatedKeys.next()) {
                        var id = generatedKeys.getInt(1);
                        return this.findById(id);
                    }
                }
            }
        }//
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

}