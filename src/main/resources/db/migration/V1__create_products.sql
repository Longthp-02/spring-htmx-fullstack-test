create table if not exists products (
    id           bigint primary key,
    title        text not null,
    handle       text not null,
    product_type text null,
    updated_at   timestamptz not null
);

create table if not exists product_variants (
    id         bigint primary key,
    product_id bigint not null references products(id) on delete cascade,
    title      text not null,
    sku        text null,
    price      numeric(12,2) null
);