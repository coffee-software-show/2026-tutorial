package com.example.beans_to_boot;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BeansToBootApplication {

    public static void main(String[] args) {

        var dr1 = new DefaultDogRepository1();
        test(dr1);

    }

    static void test(DogRepository repository) {
        repository.save("Fido", "A friendly dog");
        repository.save("Rex", "A friendly dog");
        
    }
}

interface DogRepository {
    Collection<Dog> findAll();
    
    Dog findById(int id);

    Dog save(String name, String description);
}

record Dog(int id, String name, String description) {
}

class DefaultDogRepository2 implements DogRepository {

    @Override
    public Collection<Dog> findAll() {
        return List.of();
    }

   

    @Override
    public Dog findById(int id) {
        return null;
    }

    @Override
    public Dog save(String name, String description) {
        return null;
    }
}

class DefaultDogRepository1 implements DogRepository {

    private final DataSource dataSource = new DriverManagerDataSource(
            "jdbc:postgresql://localhost:5432/mydatabase", "myuser", "secret");

    private final Collection<Dog> select(String sql) {
        var all = new ArrayList<Dog>();
        try (var c = this.dataSource.getConnection();
             var p = c.prepareStatement(sql);
             var r = p.executeQuery()) {
            while (r.next()) {
                all.add(new Dog(r.getInt("id"),
                        r.getString("name"),
                        r.getString("description")));
            }
        }// 
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return all;
    }

    @Override
    public Collection<Dog> findAll() {
        return select("select * from dog");
    }

    @Override
    public Dog findById(int id) {
        return select("select * from dog where id = " + id).iterator().next();
    }

    @Override
    public Dog save(String name, String description) {
        try (var c = this.dataSource.getConnection();
             var p = c.prepareStatement("INSERT INTO dog (name, description) VALUES (?, ?)",
                     new String[]{"id"});) {
            p.setString(1, name);
            p.setString(2, description);
            if (p.executeUpdate() > 0) {
                var id = p.getGeneratedKeys().getInt(1);
                return this.findById(id);
            }
        }//
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}