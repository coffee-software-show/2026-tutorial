package com.example.http_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import java.util.Collection;

@SpringBootApplication
public class HttpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(HttpClientApplication.class, args);
    }


}

@Component
class AnimalsClient {

    private final RestClient http;

    AnimalsClient(RestClient.Builder http) {
        this.http = http.build();
    }

    Collection<Animal> animals() {
        return this.http.get()
                .uri("http://localhost:8081/animals")
                .retrieve()
                .body(new ParameterizedTypeReference<Collection<Animal>>() {});
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