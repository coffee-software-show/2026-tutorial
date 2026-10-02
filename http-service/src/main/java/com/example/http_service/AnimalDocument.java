package com.example.http_service;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;


@Document(indexName = "animals")
record AnimalDocument(
        @Id String id,
        @Field(type = FieldType.Text) String name,
        @Field(type = FieldType.Text) String description,
        // Keyword, not Text: types are an exact-match facet, not free text to analyze
        @Field(type = FieldType.Keyword) String type) {

    static AnimalDocument from(Animal animal) {
        return new AnimalDocument(String.valueOf(animal.id()), animal.name(),
                animal.description(), animal.type().name());
    }
}
