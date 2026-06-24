create table if not exists customer
(
    id    serial primary key,
    username text not null,
    name  text not null
);

create table if not exists line_item
(
    id       serial primary key,
    sku      text    not null,
    quantity integer not null default 1,
    customer bigint references customer (id)
);

