package com.example.http_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.ImportHttpServices;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;

@SpringBootApplication
public class HttpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(HttpClientApplication.class, args);
    }

    // @Bean
    InetAddressFilter inetAddressFilter () {
        return InetAddressFilter.none() ;
    }

}



@Component
class AnimalsClient {


    private final Traverson traverson = new Traverson(
            URI.create("http://localhost:8081/"), MediaTypes.HAL_JSON);

    Collection<Animal> animals() {
        var animals = traverson
                .follow("animals")
                .toObject(new ParameterizedTypeReference<CollectionModel<EntityModel<Animal>>>() {});
        return animals.getContent()
                .stream()
                .map( em -> em.getContent())
                .toList();


    }
}

record Animal(String name, String description, int id) {
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