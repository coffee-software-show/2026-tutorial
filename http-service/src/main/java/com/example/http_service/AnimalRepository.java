package com.example.http_service;

import org.springframework.data.repository.ListCrudRepository;


interface AnimalRepository extends ListCrudRepository<Animal, Integer> {
}
