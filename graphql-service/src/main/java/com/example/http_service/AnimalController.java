package com.example.http_service;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

record TypeDetails(Animal.Type type, String wikipedia) {
}


@Controller
class AnimalController {

    private final AnimalsService animalsService;

    AnimalController(AnimalsService animalsService) {
        this.animalsService = animalsService;
    }

    @BatchMapping
    Map<Animal, TypeDetails> typeDetails(List<Animal> animals) {
        var map = new HashMap<Animal, TypeDetails>();
        animals.forEach(a -> map.put(a, this.typeDetails(a)));
        return map;
    }

    //@SchemaMapping
    private TypeDetails typeDetails(Animal animal) {
        return new TypeDetails(animal.type(),
                "https://en.wikipedia.org/wiki/" +
                        animal.name().toLowerCase());
    }

    @QueryMapping
    Collection<Animal> animals() {
        return this.animalsService.all();
    }

    @MutationMapping
    Animal create(
            @Argument String name,
            @Argument String description,
            @Argument Animal.Type type
    ) {
        return this.animalsService.add(new Animal(null, name, description, type));
    }


//    AnimalController(AnimalsService animalsService) {
//        this.animalsService = animalsService;
//    }
//
//    @GetMapping("/animals")
//    Collection<Animal> all() {
//        return this.animalsService.all();
//    }
//
//    @GetMapping("/animals/search")
//    Collection<Animal> search(@RequestParam String query) {
//        return this.animalsService.search(query);
//    }
//
//    @PostMapping("/animals")
//    Animal add(@RequestBody Animal animal) {
//        return this.animalsService.add(animal);
//    }
}

