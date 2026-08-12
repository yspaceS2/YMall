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
            WHEN 40 THEN ARRAY[
                '첫 향은 시트러스가 선명하고 시간이 지나면 우디 향이 은은하게 남아 깔끔합니다.',
                '출근할 때 두 번 정도 뿌리면 부담 없고 주변에서도 향이 산뜻하다는 말을 들었습니다.',
                '여름에 잘 어울리는 가벼운 향이지만 추운 날에는 지속력이 조금 짧게 느껴졌습니다.',
                '유리병과 분사구 마감이 단정하고 안개처럼 고르게 분사돼 사용하기 편합니다.',
                '단 향을 좋아하지 않는데 상큼하고 드라이하게 마무리되어 만족했습니다.',
                '처음에는 레몬 향이 강하지만 20분 정도 지나면 부드러운 머스크 향으로 바뀝니다.',
                '향이 강하게 퍼지는 제품은 아니라 가까운 거리에서 은은하게 느껴지는 편입니다.',
                '패키지가 깔끔해 선물용으로도 괜찮지만 상자 내부 고정은 조금 더 단단하면 좋겠습니다.',
                '옷보다 손목에 뿌렸을 때 향의 변화가 자연스럽고 잔향도 더 오래 유지됐습니다.',
                '데일리 향수로 사용하기 편하고 가격 대비 향의 완성도가 좋은 편입니다.'
            ]
            WHEN 87 THEN ARRAY[
                '베이지 색감이 화면과 비슷하고 어깨선이 자연스럽게 떨어져 단정해 보입니다.',
                '허리 벨트를 묶거나 풀었을 때 분위기가 달라 출근복과 주말 옷에 모두 활용하기 좋습니다.',
                '안감이 매끄럽고 봉제선도 깔끔했지만 처음 받았을 때 구김은 조금 있었습니다.',
                '평소 사이즈로 주문하니 얇은 니트 위에 입기 적당하고 두꺼운 옷에는 여유가 적습니다.',
                '원단이 너무 빳빳하지 않아 움직이기 편하면서도 트렌치코트 형태는 잘 유지됩니다.',
                '소매 스트랩이 자주 풀리는 점은 아쉽지만 길이와 전체 비율은 만족스럽습니다.',
                '비 오는 날 가볍게 입었는데 물방울이 바로 스며들지 않아 실용적이었습니다.',
                '무릎 아래까지 오는 길이라 바람을 잘 막아주고 간절기에 활용도가 높습니다.',
                '단추와 버클 색상이 원단과 잘 어울리고 가까이서 봐도 마감이 안정적입니다.',
                '유행을 크게 타지 않는 디자인이라 여러 해 입을 수 있을 것 같습니다.'
            ]
            WHEN 88 THEN ARRAY[
                '버건디 색상이 깊고 얼굴이 밝아 보여 가을 코디에 포인트로 입기 좋았습니다.',
                '울 혼방 원단이 따뜻하면서도 생각보다 무겁지 않아 실내외에서 편하게 입었습니다.',
                '크롭 기장이라 하이웨이스트 팬츠와 잘 맞지만 긴 상의를 선호하면 짧게 느낄 수 있습니다.',
                '어깨가 과하게 넓지 않고 소매 라인이 정돈돼 체형이 깔끔해 보입니다.',
                '안쪽 봉제와 단추 마감은 깔끔하지만 울 소재 특성상 처음에는 잔털이 조금 묻어났습니다.',
                '얇은 니트 위에는 정사이즈가 잘 맞고 여러 겹 입으려면 한 사이즈 크게 추천합니다.',
                '주머니 깊이가 충분해 휴대폰을 넣기 편하고 형태도 쉽게 흐트러지지 않습니다.',
                '카라 모양은 잘 잡히지만 목이 짧은 체형에는 위쪽 단추를 잠갔을 때 조금 답답했습니다.',
                '드라이클리닝이 필요한 점은 번거롭지만 소재와 보온성을 고려하면 납득됩니다.',
                '가격대는 있지만 색상과 실루엣이 흔하지 않아 만족도가 높습니다.'
            ]
            WHEN 110 THEN ARRAY[
                '전복이 살아 있는 상태로 도착했고 아이스박스 안의 온도도 차갑게 유지돼 있었습니다.',
                '솔로 씻은 뒤 회와 버터구이로 먹었는데 살이 단단하고 비린 맛이 거의 없었습니다.',
                '크기가 완전히 같지는 않았지만 전체 중량은 충분하고 손질 후 양도 넉넉했습니다.',
                '포장 안에 손질 방법이 함께 있어 활전복을 처음 다루는 데 도움이 됐습니다.',
                '내장이 신선해 전복죽에 활용했고 고소한 맛이 진하게 나서 만족했습니다.',
                '배송 중 바닷물이 조금 새어 비닐 포장이 한 겹 더 있으면 좋겠습니다.',
                '부모님과 함께 먹기 좋은 구성이고 특별한 날 식재료로 다시 주문하고 싶습니다.',
                '찜으로 조리해도 질기지 않고 씹을수록 단맛이 느껴졌습니다.',
                '도착 당일 바로 먹었을 때 품질이 가장 좋았고 남은 전복은 냉동 보관했습니다.',
                '가격 대비 크기와 신선도가 안정적이라 산지 배송의 장점이 느껴졌습니다.'
            ]
            WHEN 111 THEN ARRAY[
                '아이스팩이 충분히 들어 있어 연어가 차가운 상태로 도착했고 색도 선명했습니다.',
                '회로 썰어 먹었는데 비린내가 거의 없고 지방이 고르게 퍼져 부드러웠습니다.',
                '500g이라 두세 명이 먹기 충분했으며 샐러드와 덮밥으로 나눠 활용했습니다.',
                '진공 포장이 단단하고 핏물이 많지 않아 개봉 후 손질이 간편했습니다.',
                '꼬리 쪽은 조금 얇았지만 몸통 부분은 두께가 좋아 큼직하게 썰기 좋았습니다.',
                '고소한 맛은 좋았지만 기름진 부위를 싫어한다면 구워 먹는 편이 더 잘 맞겠습니다.',
                '상품 설명에 해동 방법이 자세해 냉장 해동 후 식감이 무르지 않았습니다.',
                '배송 당일 회로 먹고 남은 부분을 스테이크로 구웠는데 두 조리법 모두 만족했습니다.',
                '소스가 포함되지 않은 점은 아쉽지만 연어 자체의 신선도와 양은 좋았습니다.',
                '특별한 날 집에서 먹기 좋은 품질이라 재구매 의향이 있습니다.'
            ]
            WHEN 162 THEN ARRAY[
                '날짜가 적혀 있지 않아 중간에 쉬어도 빈 페이지 부담 없이 다시 사용할 수 있습니다.',
                '주간 일정과 메모 공간의 비율이 적당해 업무와 개인 일정을 함께 정리하기 좋았습니다.',
                '종이가 매끄럽고 젤펜 잉크가 뒤로 심하게 비치지 않아 필기감이 만족스럽습니다.',
                '한 주를 펼쳐서 볼 수 있어 계획을 빠르게 확인할 수 있지만 시간대 구분은 없습니다.',
                '표지가 단단하고 밴드가 있어 가방 안에서도 페이지가 구겨지지 않았습니다.',
                '크기는 휴대하기 좋지만 하루 기록을 길게 쓰는 분에게는 칸이 작을 수 있습니다.',
                '목표와 체크리스트 영역이 단순해 꾸준히 사용하기 편하고 디자인도 차분합니다.',
                '180도로 완전히 펴지지는 않지만 책상 위에서 필기할 때 크게 불편하지 않았습니다.',
                '월간 페이지가 함께 있어 장기 일정과 주간 할 일을 연결하기 좋았습니다.',
                '꾸미기보다 실용적인 플래너를 찾는 사람에게 잘 맞는 구성입니다.'
            ]
            WHEN 163 THEN ARRAY[
                '그리드 간격이 일정해 글씨와 도표를 함께 정리하기 좋고 세 권 구성이 실용적입니다.',
                '표지가 단단해 가방에 넣고 다녀도 모서리가 쉽게 구겨지지 않았습니다.',
                '만년필은 잉크에 따라 약간 비치지만 젤펜과 샤프는 뒷면 사용에 문제가 없었습니다.',
                '페이지가 잘 펼쳐져 코딩 메모와 아이디어 스케치를 연속해서 기록하기 편합니다.',
                '세 권의 표지 색이 달라 과목이나 프로젝트별로 구분하기 좋았습니다.',
                '미색 종이라 눈은 편하지만 선명한 백색 종이를 선호한다면 다소 탁하게 느껴질 수 있습니다.',
                '페이지 번호와 목차 영역이 없는 점은 장기 기록을 찾을 때 조금 아쉽습니다.',
                '제본이 튼튼하고 여러 번 넘겨도 낱장이 쉽게 떨어지지 않았습니다.',
                '휴대하기 좋은 대신 많은 내용을 한 권에 모으려는 사람에게는 페이지 수가 부족할 수 있습니다.',
                '가격 대비 종이와 제본 품질이 안정적이라 다 쓰면 다시 구매하고 싶습니다.'
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
            ON CONFLICT (order_item_id) DO UPDATE
            SET rating = EXCLUDED.rating,
                content = EXCLUDED.content,
                updated_at = CURRENT_TIMESTAMP
            WHERE reviews.rating IS DISTINCT FROM EXCLUDED.rating
               OR reviews.content IS DISTINCT FROM EXCLUDED.content;
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
