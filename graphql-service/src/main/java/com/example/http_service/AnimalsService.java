package com.example.http_service;

import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
class AnimalsService {

    private final AnimalRepository animalRepository;

    private final AnimalSearchRepository animalSearchRepository;

    AnimalsService(AnimalRepository animalRepository, AnimalSearchRepository animalSearchRepository) {
        this.animalRepository = animalRepository;
        this.animalSearchRepository = animalSearchRepository;
    }

    Collection<Animal> all() {
        return this.animalRepository.findAll();
    }

    Collection<Animal> search(String query) {
        var ranked = this.animalSearchRepository.search(query)
                .stream()
                .map(AnimalDocument::id)
                .map(Integer::valueOf)
                .toList();
        if (ranked.isEmpty())
            return List.of();
        var byId = this.animalRepository.findAllById(ranked)
                .stream()
                .collect(Collectors.toMap(Animal::id, Function.identity()));
        return ranked.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    Animal add(Animal animal) {
        var saved = this.animalRepository.save(animal);
        this.animalSearchRepository.save(AnimalDocument.from(saved), RefreshPolicy.IMMEDIATE);
        return saved;
    }

    void deleteAll() {
        this.animalRepository.deleteAll();
        this.animalSearchRepository.deleteAll(RefreshPolicy.IMMEDIATE);
    }


}
