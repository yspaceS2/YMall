alter table order_items
    add column delivered_at timestamp;

create table product_return_requests (
    return_request_id bigserial primary key,
    order_item_id bigint not null,
    member_id bigint not null,
    quantity integer not null,
    reason varchar(500) not null,
    status varchar(20) not null,
    seller_response varchar(500),
    payment_refund_id bigint,
    requested_at timestamp not null,
    processed_at timestamp,
    constraint fk_return_request_order_item
        foreign key (order_item_id) references order_items (id),
    constraint fk_return_request_member
        foreign key (member_id) references members (id),
    constraint ck_return_request_quantity
        check (quantity > 0)
);

create index idx_return_request_order_item
    on product_return_requests (order_item_id);

create index idx_return_request_member
    on product_return_requests (member_id, requested_at desc);

create index idx_return_request_status
    on product_return_requests (status, requested_at);
