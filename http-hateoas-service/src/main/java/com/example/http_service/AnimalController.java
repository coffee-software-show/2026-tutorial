package com.example.http_service;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Controller
@ResponseBody
class AnimalController {

    private final AnimalsService animalsService;

    private final AnimalModelAssembler assembler;

    AnimalController(AnimalsService animalsService, AnimalModelAssembler assembler) {
        this.animalsService = animalsService;
        this.assembler = assembler;
    }

    /**
     * The entry point: one bookmarkable URI from which every other affordance is
     * discoverable.
     */
    @GetMapping("/")
    RepresentationModel<?> index() {
        var model = new RepresentationModel<>();
        model.add(linkTo(methodOn(AnimalController.class).index()).withSelfRel());
        model.add(linkTo(methodOn(AnimalController.class).all()).withRel("animals"));
        model.add(linkTo(methodOn(AnimalController.class).search(null)).withRel("search"));
        return model;
    }

    @GetMapping("/animals")
    CollectionModel<EntityModel<Animal>> all() {
        return this.assembler
                .toCollectionModel(this.animalsService.all())
                .add(linkTo(methodOn(AnimalController.class).all()).withSelfRel());
    }

    @GetMapping("/animals/{id}")
    ResponseEntity<EntityModel<Animal>> byId(@PathVariable Integer id) {
        return this.animalsService.byId(id)
                .map(this.assembler::toModel)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/animals/search")
    CollectionModel<EntityModel<Animal>> search(@RequestParam String query) {
        return this.assembler
                .toCollectionModel(this.animalsService.search(query))
                .add(linkTo(methodOn(AnimalController.class).search(query)).withSelfRel())
                .add(linkTo(methodOn(AnimalController.class).all()).withRel(IanaLinkRelations.COLLECTION));
    }

    @PostMapping("/animals")
    ResponseEntity<EntityModel<Animal>> add(@RequestBody Animal animal) {
        var model = this.assembler.toModel(this.animalsService.add(animal));
        var self = model.getRequiredLink(IanaLinkRelations.SELF).toUri();
        return ResponseEntity.created(self).body(model);
    }
}
