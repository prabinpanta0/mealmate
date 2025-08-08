# MealMate

MealMate is a modern Android recipe management and meal planning application built with Kotlin and Supabase. The app helps users discover, create, organize recipes, and manage shopping lists.

![MealMate Logo](/app/src/main/res/drawable/mealmatebig.png)

## Features

- **User Authentication**: Secure login, registration, and password reset
- **Recipe Management**:
  - Create, view, edit, and delete recipes
  - Add detailed ingredients, instructions, cooking time, and servings
  - Upload and display recipe images
  - Categorize recipes
- **Home Dashboard**:
  - View suggested recipes based on meal time
  - Browse recently added recipes
  - Auto-scrolling recipe highlights
- **Shopping Lists**:
  - Create and manage shopping items
  - Add ingredients with quantities and units
- **Profile Management**: User profile customization
- **Camera Integration**: Take photos of recipes
- **Location Features**: Find nearby grocery stores and restaurants

## Tech Stack

- **Language**: Kotlin
- **Backend**: Supabase (PostgreSQL database)
- **Storage**: Supabase Storage for recipe images
- **Authentication**: Supabase Auth
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI Components**:
  - Material Design 3
  - ViewPager2
  - RecyclerView
  - Navigation Component
  - Fragments
- **Image Loading**: Glide
- **Permissions**: Camera, Location, Storage, Contacts, SMS

## Database Schema

The app uses Supabase with the following tables:
- `profiles`: User profile information
- `recipes`: Recipe details including title, instructions, and images
- `shopping_lists`: User shopping lists

## Setup

1. Clone the repository:
   ```
   git clone https://github.com/prabinpanta0/mealmate.git
   ```

2. Open the project in Android Studio

3. Configure your Supabase credentials in the appropriate configuration file (not included in the repository)

4. Build and run the application on an Android device or emulator

## Requirements

- Android SDK 21+
- Kotlin 1.8+
- Internet connection for Supabase backend functionality

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

- Prabin Panta ([@prabinpanta0](https://github.com/prabinpanta0))
