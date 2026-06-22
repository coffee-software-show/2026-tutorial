create table if not exists customer
(
    id    serial primary key,
    email text not null,
    name  text not null
);

-- create table if not exists orders
-- (
--     id          serial primary key,
--     customer_id bigint references customers (id)
-- );
--
create table if not exists line_item
(
    id       serial primary key,
    sku      text    not null,
    quantity integer not null default 1,
    customer bigint references customer (id)
);