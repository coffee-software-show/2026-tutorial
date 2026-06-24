package com.example.v3;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

import java.lang.annotation.*;
import java.util.Collection;

@SpringBootApplication
public class CustomersApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomersApplication.class, args);
    }

    @EventListener
    void afterPropertiesSet(ApplicationEvent event) {
        IO.println("event fired: " +
                event.toString());
    }
}

@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;

    AssistantController(ChatClient.Builder ai) {
        this.ai = ai.build();
    }

    @GetMapping("/ask")
    String ask(@RequestParam String question) {
        return this.ai
                .prompt()
                .user(question)
                .call()
                .content();
    }
}

@Controller
@ResponseBody
class DemoController {

    private final RestClient http;

    DemoController(RestClient.Builder http) {
        this.http = http.build();
    }

    @GetMapping("/delay")
    String delay() {
        var threads = Thread.currentThread() + ":::";
        var response = http.get().uri("http://localhost:9000/delay/5").retrieve().body(String.class);
        threads += Thread.currentThread();
        IO.println(">" + threads + "<");
        return response;
    }
}

@Controller
@ResponseBody
class DogsController {

    private final DogRepository dogRepository;

    DogsController(DogRepository dogRepository) {
        this.dogRepository = dogRepository;
    }

    @GetMapping("/dogs")
    Collection<Dog> dogs() {
        IO.println("dogs called");
        return dogRepository.findAll();
    }
}


@Component
class DogsApplicationRunner implements ApplicationRunner {

    private final DogRepository dogRepository;

    DogsApplicationRunner(DogRepository dogRepository) {
        this.dogRepository = dogRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        dogRepository.findAll().forEach(IO::println);
    }
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@interface MizuhoRepository {

    /**
     * Alias for {@link Component#value}.
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

//@MizuhoRepository
/*
@Transactional
class DogRepository {

    private final JdbcClient db;

    DogRepository(JdbcClient db) {
        this.db = db;
    }

    public Collection<Dog> findAll() {
        return this.db //
                .sql("select * from dog") //
                .query((rs, _) -> new Dog(rs.getInt("id"), rs.getString("name")))//
                .list();
    }
}
*/

record Dog(int id, String name) {
}


// spring boot
// > autoconfiguration


