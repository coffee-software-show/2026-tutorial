package com.example.http_service;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Turns an {@link Animal} into a representation that carries its own links, so clients
 * navigate by relation instead of hardcoding URI templates.
 */
@Component
class AnimalModelAssembler implements RepresentationModelAssembler<Animal, EntityModel<Animal>> {

    @Override
    public EntityModel<Animal> toModel(Animal animal) {
        return EntityModel.of(animal,
                linkTo(methodOn(AnimalController.class).byId(animal.id())).withSelfRel(),
                linkTo(methodOn(AnimalController.class).all()).withRel(IanaLinkRelations.COLLECTION));
    }
}
