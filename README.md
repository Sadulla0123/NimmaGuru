# Nimma-Guru

Kotlin Android implementation of the Nimma-Guru PRD: a community mentorship app that connects local students with retired professionals and volunteers.

## Implemented MVP

- Guru Directory with sample mentor profiles.
- Skill filter chips and locality search.
- Guru profile cards with skills, languages, bio, free hours, contact, and appreciation notes.
- Student actions: session request and appreciation posting.
- Firestore listeners for Guru profiles, class calendar, appreciation updates, Wall of Fame ranking, and student action history.
- Profile edit flow for skills, availability, and contact details.
- Wall of Fame ranked by appreciation count.
- Class Calendar with upcoming community sessions.
- English and Kannada string resources with an in-app language toggle.
- Jetpack Compose UI with ViewModel and Repository separation.

## Project Notes

- Minimum SDK: 21.
- Compile SDK: 35.
- Kotlin Android plugin: 2.0.21.
- Android Gradle plugin: 8.7.3.
- Database: Cloud Firestore.
- Auth: Firebase anonymous sign-in for student actions.

## Firebase Setup

1. Create a Firebase Android app with package name `com.nimmaguru.app`.
2. Download `google-services.json`.
3. Put it at `app/google-services.json`.
4. Enable Firebase Authentication with Anonymous sign-in.
5. Enable Cloud Firestore.
6. Publish rules from `firestore.rules`, or copy them into the Firebase console.
7. Sync and run from Android Studio.

On first launch, the app seeds sample `gurus` and `classes` documents if the `gurus` collection is empty.

Firestore collections used:

- `gurus`
- `classes`
- `student_actions`
