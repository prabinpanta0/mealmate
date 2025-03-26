-- Table definitions with proper relationships and security
CREATE TABLE profiles (
  id UUID REFERENCES auth.users PRIMARY KEY,
  email TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    preparation_time INTEGER,
    ingredients JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    description TEXT,
    instructions TEXT,
    servings INT,
    image TEXT CHECK (image ~ '^https?://[^\s/$.?#].[^\s]*$')
);

INSERT INTO storage.buckets (id, name, public)
VALUES ('recipes', 'recipes', true);

CREATE POLICY "Recipes image access" ON storage.objects
FOR SELECT USING (bucket_id = 'recipes' AND auth.uid() = owner);

CREATE POLICY "Recipes image upload" ON storage.objects
FOR INSERT WITH CHECK (bucket_id = 'recipes' AND auth.role() = 'authenticated' AND owner = auth.uid());

CREATE TABLE ingredients (
  id UUID PRIMARY KEY,
  recipe_id UUID REFERENCES recipes(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  category TEXT,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE shopping_lists (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
  items JSONB NOT NULL,
  completed BOOLEAN DEFAULT false,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Row Level Security Policies
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "User can manage their profile" 
ON profiles FOR ALL USING (auth.uid() = id);

ALTER TABLE recipes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "User can manage their recipes"
ON recipes FOR ALL USING (auth.uid() = user_id);

ALTER TABLE shopping_lists ENABLE ROW LEVEL SECURITY;
CREATE POLICY "User can manage their shopping lists"
ON shopping_lists FOR ALL USING (auth.uid() = user_id);

-- Indexes for common queries
CREATE INDEX idx_recipes_user ON recipes(user_id);
CREATE INDEX idx_shopping_lists_user ON shopping_lists(user_id);