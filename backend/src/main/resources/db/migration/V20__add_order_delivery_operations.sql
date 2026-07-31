alter table orders
    add column delivery_request varchar(100),
    add column delivered_at timestamp;

alter table order_items
    add column shipping_carrier varchar(30),
    add column tracking_number varchar(50);

update orders
set delivered_at = updated_at
where status = 'DELIVERED'
  and delivered_at is null;
