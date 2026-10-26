package com.example.http_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import java.util.Collection;

@SpringBootApplication
public class GraphqlClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphqlClientApplication.class, args);
    }

    // @Bean
    InetAddressFilter inetAddressFilter() {
        return InetAddressFilter.none();
    }

}

record Animal(String name,
              String description, TypeDetails typeDetails,
              int id) {
}

@Component
class AnimalsClient {

    private final GraphQlClient httpGraphQlClient;

    AnimalsClient(RestClient.Builder http) {
        this.httpGraphQlClient = HttpSyncGraphQlClient
                .builder(http.build())
                .url("http://localhost:8081/graphql")
                .build();
    }

    Collection<Animal> animals() {
        return this.httpGraphQlClient
                .document("""
                        {
                          animals {
                            id, name, typeDetails { type, wikipedia }, description
                          }
                        }
                        """)
                .retrieveSync("animals")
                .toEntityList(Animal.class);
    }
}

record TypeDetails(String type, String wikipedia) {
}

@Controller
class AnimalsClientController {

    private final AnimalsClient animalsClient;

    AnimalsClientController(AnimalsClient animalsClient) {
        this.animalsClient = animalsClient;
    }

    @GetMapping("/animals.html")
    String animals(Model mav) {
        mav.addAttribute("animals", this.animalsClient.animals());
        return "animals";
    }
}