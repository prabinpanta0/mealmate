-- Fix recipe security policies to allow reading other users' recipes
-- First, drop the existing policy
DROP POLICY IF EXISTS "User can manage their recipes" ON recipes;

-- Create separate policies for different operations
-- Allow users to read any recipe (SELECT)
CREATE POLICY "Users can read any recipe" 
ON recipes 
FOR SELECT 
USING (true);

-- Allow users to create their own recipes (INSERT)
CREATE POLICY "Users can create their own recipes" 
ON recipes 
FOR INSERT 
WITH CHECK (auth.uid() = user_id);

-- Allow users to update only their own recipes (UPDATE)
CREATE POLICY "Users can update their own recipes" 
ON recipes 
FOR UPDATE 
USING (auth.uid() = user_id);

-- Allow users to delete only their own recipes (DELETE)
CREATE POLICY "Users can delete their own recipes" 
ON recipes 
FOR DELETE 
USING (auth.uid() = user_id);