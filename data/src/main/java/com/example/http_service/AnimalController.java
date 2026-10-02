package com.example.http_service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;

@Controller
@ResponseBody
class AnimalController {

    private final AnimalsService animalsService;

    AnimalController(AnimalsService animalsService) {
        this.animalsService = animalsService;
    }

    @GetMapping("/animals")
    Collection<Animal> all() {
        return this.animalsService.all();
    }

    @GetMapping("/animals/search")
    Collection<Animal> search(@RequestParam String query) {
        return this.animalsService.search(query);
    }

    @GetMapping("/animals/by-type")
    Collection<Animal> byType(@RequestParam Animal.Type type) {
        return this.animalsService.byType(type);
    }

    @PostMapping("/animals")
    Animal add(@RequestBody Animal animal) {
        return this.animalsService.add(animal);
    }
}

