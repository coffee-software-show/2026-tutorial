package com.example.http_service;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table//("animal")
record Animal(@Id Integer id, String name, String description, Type type) {

    enum Type {
        DOG,
        CAT,
        BIRD,
        FISH,
        REPTILE,
        OTHER
    }
}
