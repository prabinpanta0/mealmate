-- Drop existing storage policies
DROP POLICY IF EXISTS "Recipes image access" ON storage.objects;
DROP POLICY IF EXISTS "Recipes image upload" ON storage.objects;

-- Allow authenticated users to read any recipe image
CREATE POLICY "Anyone can read recipe images"
ON storage.objects FOR SELECT
USING (bucket_id = 'recipes');

-- Allow authenticated users to upload and update their recipe images
CREATE POLICY "Authenticated users can upload recipe images"
ON storage.objects FOR INSERT
WITH CHECK (
    bucket_id = 'recipes' 
    AND auth.role() = 'authenticated'
);

-- Allow users to update their own recipe images
CREATE POLICY "Users can update their recipe images"
ON storage.objects FOR UPDATE
USING (
    bucket_id = 'recipes'
    AND auth.role() = 'authenticated'
    AND owner = auth.uid()
);

-- Allow users to delete their own recipe images
CREATE POLICY "Users can delete their recipe images"
ON storage.objects FOR DELETE
USING (
    bucket_id = 'recipes'
    AND auth.role() = 'authenticated'
    AND owner = auth.uid()
);