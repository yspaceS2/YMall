\set ON_ERROR_STOP on

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    target_ids bigint[] := ARRAY[40, 87, 88, 95, 103, 110, 111, 114, 123, 143, 161, 162, 163];
    manual_target_ids bigint[] := ARRAY[123, 114, 143, 95, 161, 103];
    fixture_ratings integer[] := ARRAY[5, 4, 5, 4, 5, 3, 5, 4, 5, 4];
    v_product_id bigint;
    v_product_name text;
    v_unit_price numeric(12, 2);
    v_index integer;
    v_fixture_count integer;
    v_reviewer_id bigint;
    v_order_id bigint;
    v_order_item_id bigint;
    v_review_texts text[];
    v_demo_member_id bigint;
    v_admin_member_id bigint;
    v_demo_order_id bigint;
    v_demo_total numeric(38, 2);
BEGIN
    IF EXISTS (
        SELECT 1
        FROM unnest(target_ids) AS target(id)
        LEFT JOIN products p ON p.id = target.id
        WHERE p.id IS NULL
    ) THEN
        RAISE EXCEPTION 'A target demo product is missing.';
    END IF;

    SELECT m.id INTO v_admin_member_id
    FROM members m
    WHERE m.email = 'admin@ymall.cloud'
      AND m.role = 'ROLE_ADMIN';

    IF v_admin_member_id IS NULL THEN
        RAISE EXCEPTION 'The demo super administrator does not exist.';
    END IF;

    FOR v_index IN 1..10 LOOP
        INSERT INTO members (
            created_at, updated_at, email, name, password, role,
            email_verified_at, auth_version, access_status,
            restriction_reason, restricted_at, restricted_by
        )
        VALUES (
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP,
            format('reviewer%s@demo.ymall.cloud', lpad(v_index::text, 2, '0')),
            format('데모 리뷰어 %s', lpad(v_index::text, 2, '0')),
            crypt(gen_random_uuid()::text, gen_salt('bf', 10)),
            'ROLE_USER',
            CURRENT_TIMESTAMP,
            0,
            'RESTRICTED',
            '포트폴리오 리뷰 fixture 전용 계정',
            CURRENT_TIMESTAMP,
            v_admin_member_id
        )
        ON CONFLICT (email) DO NOTHING;
    END LOOP;

    FOREACH v_product_id IN ARRAY target_ids LOOP
        SELECT
            p.name,
            round(p.price * (100 - COALESCE(p.discount_percentage, 0)) / 100, 2)
        INTO v_product_name, v_unit_price
        FROM products p
        WHERE p.id = v_product_id;

        v_fixture_count := CASE
            WHEN v_product_id = 123 THEN 8
            WHEN v_product_id = ANY(manual_target_ids) THEN 9
            ELSE 10
        END;
        v_review_texts := CASE v_product_id
            WHEN 123 THEN ARRAY[
                '개별 파우치라 출근 전에 챙기기 편하고 포장이 단단해서 보관하기 좋았습니다.',
                '양배추 향이 생각보다 순해서 매일 마시기 부담 없었고 단맛이 과하지 않았습니다.',
                '배송이 빠르고 상자 눌림 없이 도착했습니다. 차갑게 마시면 더 깔끔합니다.',
                '한 포 양이 적당하고 휴대하기 좋습니다. 할인할 때 재구매할 생각입니다.',
                '원재료 구성이 단순해서 선택했습니다. 처음에는 향이 조금 낯설 수 있습니다.',
                '아침 식사 전에 꾸준히 챙기기 좋았습니다. 절취선도 잘 뜯어집니다.',
                '가족과 나눠 마시기 좋은 구성입니다. 냉장 보관 후 마시는 편이 좋았습니다.',
                '포장과 유통기한 표기가 선명하고 깔끔했습니다. 전체적으로 만족스럽습니다.'
            ]
            WHEN 114 THEN ARRAY[
                '국물이 진하면서도 짜지 않아 간단한 한 끼로 먹기 좋았습니다.',
                '파우치째 데울 수 있어 편하고 고기 건더기도 생각보다 넉넉했습니다.',
                '배송 중 냉동 상태가 잘 유지됐고 포장 누수 없이 도착했습니다.',
                '대파와 후추만 추가해도 식당에서 먹는 것처럼 든든했습니다.',
                '간은 담백한 편이라 취향에 맞게 소금을 더할 수 있어 좋았습니다.',
                '한 사람이 먹기 알맞지만 많이 먹는 분에게는 조금 부족할 수 있습니다.',
                '바쁜 날 빠르게 준비할 수 있고 잡내가 거의 없어 만족했습니다.',
                '다섯 팩 구성이라 냉동실에 보관해 두고 활용하기 편했습니다.',
                '가격 대비 맛과 포장이 안정적이라 다음에도 구매하고 싶습니다.'
            ]
            WHEN 143 THEN ARRAY[
                '나뭇결과 색감이 화면과 비슷하고 거실 분위기가 차분해졌습니다.',
                '슬랫 각도 조절이 부드럽고 햇빛 차단 정도를 세밀하게 맞출 수 있습니다.',
                '포장이 꼼꼼해 모서리 손상 없이 도착했고 부속품도 잘 정리돼 있었습니다.',
                '설치 설명은 쉬웠지만 폭이 넓으면 두 사람이 작업하는 편이 안전합니다.',
                '우드 소재라 무게감은 있으나 설치 후 흔들림이 적고 안정적입니다.',
                '빛이 완전히 차단되지는 않지만 자연스러운 채광 조절에는 만족스럽습니다.',
                '마감이 깔끔하고 줄 조작감이 좋아 매일 사용하기 편합니다.',
                '사이즈 측정을 정확히 해야 하지만 맞춤 느낌이 좋아 결과는 만족스럽습니다.',
                '가격대는 있지만 소재와 완성도를 고려하면 납득할 만합니다.'
            ]
            WHEN 95 THEN ARRAY[
                '가죽이 부드럽고 첫날부터 발등이 크게 불편하지 않았습니다.',
                '깔끔한 디자인이라 청바지와 슬랙스 모두 잘 어울립니다.',
                '밑창 쿠션이 적당해 출퇴근하면서 오래 걸어도 편했습니다.',
                '정사이즈로 맞았지만 발볼이 넓다면 반 사이즈 크게 신는 편이 좋겠습니다.',
                '박스와 신발 포장이 깔끔했고 가죽 표면에 흠집 없이 도착했습니다.',
                '화이트 색상이라 오염 관리는 필요하지만 닦아내기는 어렵지 않았습니다.',
                '마감선이 정돈돼 있고 뒤꿈치가 안정적으로 잡혀 만족했습니다.',
                '아주 가볍지는 않지만 착화감과 균형은 좋은 편입니다.',
                '할인 가격 기준으로 디자인과 소재 모두 만족스러운 선택이었습니다.'
            ]
            WHEN 161 THEN ARRAY[
                '샤프와 홀더의 무게 중심이 좋아 오래 필기해도 손이 덜 피곤했습니다.',
                '금속 바디 마감이 깔끔하고 구성품이 케이스 안에 잘 정리돼 있습니다.',
                '선 굵기를 바꿔 스케치하기 편하며 그립이 미끄럽지 않았습니다.',
                '필기감은 단단한 편이라 부드러운 촉감을 선호하면 적응이 필요합니다.',
                '선물용으로도 괜찮을 만큼 포장이 정돈돼 있고 디자인이 고급스럽습니다.',
                '홀더 심 교체가 간단하고 기본 제공 심도 일상 사용에는 충분했습니다.',
                '휴대 케이스가 튼튼하지만 전체 무게는 일반 필기구보다 조금 무겁습니다.',
                '정밀한 드로잉 작업에서 선이 흔들리지 않아 유용했습니다.',
                '가격은 보급형보다 높지만 내구성과 구성을 고려하면 만족스럽습니다.'
            ]
            WHEN 103 THEN ARRAY[
                '접지력이 좋아 흙길과 젖은 노면에서 발이 안정적으로 느껴졌습니다.',
                '발목을 단단히 잡아주면서도 앞부분은 유연해 달리기 편했습니다.',
                '통풍이 잘되고 장시간 착용해도 내부가 답답하지 않았습니다.',
                '일반 운동화보다 밑창이 단단해 처음에는 적응 시간이 조금 필요했습니다.',
                '정사이즈에 가깝지만 두꺼운 양말을 신으면 여유가 적습니다.',
                '포장 상태가 좋고 신발 양쪽 마감과 접착 상태도 균일했습니다.',
                '오르막과 내리막에서 미끄러짐이 적어 트레일 입문용으로 만족했습니다.',
                '쿠션이 지나치게 푹신하지 않아 장거리에서 발이 안정적이었습니다.',
                '디자인과 기능의 균형이 좋고 할인 가격이라면 재구매할 의향이 있습니다.'
            ]
            ELSE ARRAY[
                format('%s 상품은 설명과 실제 구성이 잘 맞아 만족스럽습니다.', v_product_name),
                '포장 상태가 깔끔했고 배송 과정에서 손상된 부분이 없었습니다.',
                '처음 사용해도 어렵지 않았고 기본적인 완성도가 좋았습니다.',
                '가격과 품질을 함께 고려하면 합리적인 선택이라고 생각합니다.',
                '디자인과 마감이 단정해서 일상적으로 사용하기 좋습니다.',
                '기대했던 기능을 안정적으로 제공해 전반적으로 만족합니다.',
                '사용 방법과 상품 정보가 조금 더 자세하면 좋겠습니다.',
                '배송이 빠르고 구성품도 빠짐없이 도착했습니다.',
                '비슷한 상품과 비교했을 때 품질이 준수하고 재구매 의향이 있습니다.',
                '전체적으로 만족하지만 선택 가능한 옵션이 더 다양하면 좋겠습니다.'
            ]
        END;

        FOR v_index IN 1..v_fixture_count LOOP
            SELECT m.id INTO v_reviewer_id
            FROM members m
            WHERE m.email = format(
                'reviewer%s@demo.ymall.cloud',
                lpad(v_index::text, 2, '0')
            );

            INSERT INTO orders (
                created_at, updated_at, member_id, idempotency_key,
                payment_order_id, status, total_amount, shipping_amount,
                inventory_reserved, recipient_name, recipient_phone,
                postal_code, road_address, detail_address, delivered_at
            )
            VALUES (
                CURRENT_TIMESTAMP - make_interval(days => 70 - v_index),
                CURRENT_TIMESTAMP - make_interval(days => 69 - v_index),
                v_reviewer_id,
                format('demo-review-%s-%s', v_product_id, v_index),
                format('DEMO-%s-%s', v_product_id, v_index),
                'DELIVERED',
                v_unit_price,
                0,
                false,
                format('데모 리뷰어 %s', lpad(v_index::text, 2, '0')),
                '01000000000',
                '06133',
                '서울 강남구 테헤란로 123',
                'YMall 리뷰 fixture',
                CURRENT_TIMESTAMP - make_interval(days => 69 - v_index)
            )
            ON CONFLICT (member_id, idempotency_key) DO NOTHING;

            SELECT o.id INTO v_order_id
            FROM orders o
            WHERE o.member_id = v_reviewer_id
              AND o.idempotency_key = format(
                  'demo-review-%s-%s', v_product_id, v_index
              );

            INSERT INTO order_items (
                order_id, product_id, product_name, unit_price, quantity,
                line_total, shipping_fee, refunded_quantity,
                fulfillment_status, carrier, tracking_number,
                shipped_at, delivered_at
            )
            SELECT
                v_order_id, v_product_id, v_product_name, v_unit_price, 1,
                v_unit_price, 0, 0, 'DELIVERED', 'YMall 데모택배',
                format('FIXTURE-%s-%s', v_product_id, v_index),
                CURRENT_TIMESTAMP - make_interval(days => 70 - v_index),
                CURRENT_TIMESTAMP - make_interval(days => 69 - v_index)
            WHERE NOT EXISTS (
                SELECT 1 FROM order_items oi
                WHERE oi.order_id = v_order_id
                  AND oi.product_id = v_product_id
            );

            SELECT oi.id INTO v_order_item_id
            FROM order_items oi
            WHERE oi.order_id = v_order_id
              AND oi.product_id = v_product_id;

            INSERT INTO reviews (
                member_id, product_id, order_item_id, rating, content,
                created_at, updated_at
            )
            VALUES (
                v_reviewer_id,
                v_product_id,
                v_order_item_id,
                fixture_ratings[v_index],
                v_review_texts[v_index],
                CURRENT_TIMESTAMP - make_interval(days => 68 - v_index),
                CURRENT_TIMESTAMP - make_interval(days => 68 - v_index)
            )
            ON CONFLICT (order_item_id) DO NOTHING;
        END LOOP;
    END LOOP;

    SELECT m.id INTO v_demo_member_id
    FROM members m
    WHERE m.email = 'user@ymall.cloud';

    IF v_demo_member_id IS NULL THEN
        RAISE EXCEPTION 'The public demo member does not exist.';
    END IF;

    SELECT COALESCE(sum(
        round(p.price * (100 - COALESCE(p.discount_percentage, 0)) / 100, 2)
    ), 0)
    INTO v_demo_total
    FROM products p
    WHERE p.id = ANY(manual_target_ids);

    INSERT INTO orders (
        created_at, updated_at, member_id, idempotency_key,
        payment_order_id, status, total_amount, shipping_amount,
        inventory_reserved, recipient_name, recipient_phone,
        postal_code, road_address, detail_address, delivered_at
    )
    VALUES (
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        v_demo_member_id,
        'demo-review-final-user',
        'DEMO-REVIEW-FINAL-USER',
        'DELIVERED',
        v_demo_total,
        0,
        false,
        'YMall 데모 회원',
        '01023572357',
        '06133',
        '서울 강남구 테헤란로 123',
        'YMall 포트폴리오 데모',
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (member_id, idempotency_key) DO NOTHING;

    SELECT o.id INTO v_demo_order_id
    FROM orders o
    WHERE o.member_id = v_demo_member_id
      AND o.idempotency_key = 'demo-review-final-user';

    FOREACH v_product_id IN ARRAY manual_target_ids LOOP
        SELECT
            p.name,
            round(p.price * (100 - COALESCE(p.discount_percentage, 0)) / 100, 2)
        INTO v_product_name, v_unit_price
        FROM products p
        WHERE p.id = v_product_id;

        INSERT INTO order_items (
            order_id, product_id, product_name, unit_price, quantity,
            line_total, shipping_fee, refunded_quantity,
            fulfillment_status, carrier, tracking_number,
            shipped_at, delivered_at
        )
        SELECT
            v_demo_order_id, v_product_id, v_product_name, v_unit_price, 1,
            v_unit_price, 0, 0, 'DELIVERED', 'YMall 데모택배',
            format('FINAL-%s', v_product_id), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1 FROM order_items oi
            WHERE oi.order_id = v_demo_order_id
              AND oi.product_id = v_product_id
        );
    END LOOP;

    UPDATE products p
    SET rating = aggregate.average_rating,
        updated_at = CURRENT_TIMESTAMP
    FROM (
        SELECT r.product_id, round(avg(r.rating)::numeric, 2) AS average_rating
        FROM reviews r
        WHERE r.product_id = ANY(target_ids)
        GROUP BY r.product_id
    ) AS aggregate
    WHERE p.id = aggregate.product_id;
END
$$;

COMMIT;

SELECT
    p.id AS product_id,
    p.name,
    count(r.id) AS review_count,
    p.rating
FROM products p
LEFT JOIN reviews r ON r.product_id = p.id
WHERE p.id IN (40, 87, 88, 95, 103, 110, 111, 114, 123, 143, 161, 162, 163)
GROUP BY p.id, p.name, p.rating
ORDER BY p.id;
