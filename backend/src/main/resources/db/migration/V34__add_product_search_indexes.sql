CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION ymall_product_chosung(input_text TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
STRICT
AS $$
DECLARE
    initials TEXT[] := ARRAY[
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    ];
    normalized_text TEXT := lower(regexp_replace(input_text, '[[:space:]]', '', 'g'));
    result_text TEXT := '';
    current_character TEXT;
    code_point INTEGER;
    character_index INTEGER;
BEGIN
    IF normalized_text = '' THEN
        RETURN '';
    END IF;

    FOR character_index IN 1..char_length(normalized_text) LOOP
        current_character := substr(normalized_text, character_index, 1);
        code_point := ascii(current_character);

        IF code_point BETWEEN 44032 AND 55203 THEN
            result_text := result_text
                || initials[((code_point - 44032) / 588) + 1];
        ELSIF current_character ~ '^[ㄱ-ㅎ[:alnum:]]$' THEN
            result_text := result_text || current_character;
        END IF;
    END LOOP;

    RETURN result_text;
END;
$$;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS search_normalized_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS search_chosung VARCHAR(255);

UPDATE products
SET search_normalized_name = lower(regexp_replace(name, '[[:space:]]', '', 'g')),
    search_chosung = ymall_product_chosung(name)
WHERE search_normalized_name IS NULL
   OR search_chosung IS NULL;

ALTER TABLE products
    ALTER COLUMN search_normalized_name SET NOT NULL,
    ALTER COLUMN search_chosung SET NOT NULL;

CREATE OR REPLACE FUNCTION ymall_update_product_search_fields()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.search_normalized_name := lower(regexp_replace(NEW.name, '[[:space:]]', '', 'g'));
    NEW.search_chosung := ymall_product_chosung(NEW.name);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_products_search_fields ON products;
CREATE TRIGGER trg_products_search_fields
BEFORE INSERT OR UPDATE OF name ON products
FOR EACH ROW
EXECUTE FUNCTION ymall_update_product_search_fields();

CREATE INDEX IF NOT EXISTS idx_products_search_normalized_name_trgm
    ON products USING gin (search_normalized_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_search_chosung_trgm
    ON products USING gin (search_chosung gin_trgm_ops);
