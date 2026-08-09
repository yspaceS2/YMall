alter table orders
    add column shipping_amount numeric(38, 2) not null default 0;

alter table order_items
    add column shipping_fee numeric(12, 2) not null default 0;
