create table if not exists customers
(
    id    serial primary key,
    name  varchar(255),
    email varchar(255)
);