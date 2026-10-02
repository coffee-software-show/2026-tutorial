package com.example.http_service;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * The Elasticsearch projection of an {@link Animal}. Postgres owns the data; this is a
 * derived index that exists purely to be searched, and can be rebuilt at any time.
 *
 * <p>The {@code @Document} annotation is also what lets Spring Data tell the two stores
 * apart: with multiple Spring Data modules on the classpath, repository scanning runs in
 * strict mode and assigns each repository to the module that recognizes its entity.
 */
@Document(indexName = "animals")
record AnimalDocument(

        // Elasticsearch _id values are always strings, hence the widening from Integer
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
