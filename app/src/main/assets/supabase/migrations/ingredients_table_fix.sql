-- Migration to fix ingredients table structure

-- First, drop the existing ingredients table
DROP TABLE IF EXISTS ingredients;

-- Recreate the ingredients table with the correct structure
CREATE TABLE ingredients (
  id UUID PRIMARY KEY,
  recipe_id UUID REFERENCES recipes(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  category TEXT,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create an index on recipe_id for faster lookups
CREATE INDEX idx_ingredients_recipe ON ingredients(recipe_id);

-- Add row level security policy
ALTER TABLE ingredients ENABLE ROW LEVEL SECURITY;

-- Create policy to allow users to manage their ingredients through recipe ownership
CREATE POLICY "Users can manage ingredients of their recipes" 
ON ingredients 
FOR ALL 
USING (
  EXISTS (
    SELECT 1 FROM recipes 
    WHERE recipes.id = ingredients.recipe_id 
    AND recipes.user_id = auth.uid()
  )
);