create table animal
(
    id          serial primary key,
    name        text not null,
    description text not null,
    owner       text not null,
    type        text not null
);