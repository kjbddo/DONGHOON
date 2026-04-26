-- citext + JdbcTypeCode(OTHER) 가 JPQL/파라미터 바인딩에서 실패(AdminAccountSeeder existsByEmail 등)하므로 varchar 로 통일
-- 대소문자 구분 unique 가 필요하면 별도 unique index (lower(email)) 를 고려
ALTER TABLE users
    ALTER COLUMN email TYPE varchar(255) USING email::varchar(255);
