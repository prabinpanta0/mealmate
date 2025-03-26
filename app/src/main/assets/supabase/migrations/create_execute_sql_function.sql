-- Create a stored procedure that can execute arbitrary SQL
-- This should be a separate migration to run first
CREATE OR REPLACE FUNCTION execute_sql(query text)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER -- Run with privileges of the function owner
AS $$
BEGIN
  EXECUTE query;
END;
$$;