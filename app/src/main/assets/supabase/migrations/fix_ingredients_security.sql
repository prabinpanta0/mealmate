-- Fix ingredients security policies to allow reading ingredients of any recipe
-- First, drop the existing policy
DROP POLICY IF EXISTS "Users can manage ingredients of their recipes" ON ingredients;

-- Create separate policies for different operations
-- Allow users to read ingredients of any recipe (SELECT)
CREATE POLICY "Users can read ingredients of any recipe" 
ON ingredients 
FOR SELECT 
USING (true);

-- Allow users to create ingredients only for their own recipes (INSERT)
CREATE POLICY "Users can create ingredients for their recipes" 
ON ingredients 
FOR INSERT 
WITH CHECK (
    EXISTS (
        SELECT 1 FROM recipes 
        WHERE recipes.id = recipe_id 
        AND recipes.user_id = auth.uid()
    )
);

-- Allow users to update ingredients only for their own recipes (UPDATE)
CREATE POLICY "Users can update ingredients of their recipes" 
ON ingredients 
FOR UPDATE 
USING (
    EXISTS (
        SELECT 1 FROM recipes 
        WHERE recipes.id = recipe_id 
        AND recipes.user_id = auth.uid()
    )
);

-- Allow users to delete ingredients only from their own recipes (DELETE)
CREATE POLICY "Users can delete ingredients from their recipes" 
ON ingredients 
FOR DELETE 
USING (
    EXISTS (
        SELECT 1 FROM recipes 
        WHERE recipes.id = recipe_id 
        AND recipes.user_id = auth.uid()
    )
);