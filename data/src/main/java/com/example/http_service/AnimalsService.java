package com.example.http_service;

import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
        return hydrate(ranked);
    }

    Collection<Animal> byType(Animal.Type type) {
        var ranked = this.animalSearchRepository.findByType(type.name())
                .stream()
                .map(AnimalDocument::id)
                .map(Integer::valueOf)
                .toList();
        return hydrate(ranked);
    }

    /**
     * Postgres first, so the generated id is settled before anything is indexed; then
     * project into Elasticsearch. IMMEDIATE makes the write searchable now instead of
     * after the index's next refresh (~1s), which otherwise makes write-then-search
     * look broken.
     */
    Animal add(Animal animal) {
        var saved = this.animalRepository.save(animal);
        this.animalSearchRepository.save(AnimalDocument.from(saved), RefreshPolicy.IMMEDIATE);
        return saved;
    }

    void deleteAll() {
        this.animalRepository.deleteAll();
        this.animalSearchRepository.deleteAll(RefreshPolicy.IMMEDIATE);
    }

    void reindex() {
        var documents = this.animalRepository.findAll().stream().map(AnimalDocument::from).toList();
        this.animalSearchRepository.deleteAll(RefreshPolicy.IMMEDIATE);
        this.animalSearchRepository.saveAll(documents, RefreshPolicy.IMMEDIATE);
    }


    private Collection<Animal> hydrate(List<Integer> rankedIds) {
        if (rankedIds.isEmpty())
            return List.of();
        var byId = this.animalRepository.findAllById(rankedIds)
                .stream()
                .collect(Collectors.toMap(Animal::id, Function.identity()));
        return rankedIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }
}
