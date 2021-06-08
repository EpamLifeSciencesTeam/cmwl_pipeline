DO
$func$
BEGIN
EXECUTE
    (SELECT 'TRUNCATE TABLE ' || string_agg(oid::regclass::text, ', ') || ' CASCADE'
     FROM pg_class
     WHERE relkind = 'r' -- 'r' stands for "ordinary table" kind
       AND relname NOT IN ('databasechangelog', 'databasechangeloglock')
       AND relnamespace = 'public'::regnamespace
    );
END
$func$;
