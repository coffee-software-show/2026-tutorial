create table if not exists animal
(
    id          serial primary key,
    name        text not null,
    type        text not null,
    description text not null
);