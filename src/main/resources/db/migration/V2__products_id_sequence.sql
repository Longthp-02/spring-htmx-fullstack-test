create sequence if not exists products_manual_id_seq;

alter table products
    alter column id set default nextval('products_manual_id_seq');

do $$
declare
    max_id bigint;
begin
    select max(id) into max_id from products;

    if max_id is null then
        perform setval('products_manual_id_seq', 1, false);
    else
        perform setval('products_manual_id_seq', max_id, true);
    end if;
end
$$;