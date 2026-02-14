create extension if not exists pg_trgm;

create index if not exists idx_products_title_trgm
    on products using gin (title gin_trgm_ops);

create index if not exists idx_products_updated_at_desc
    on products (updated_at desc);
