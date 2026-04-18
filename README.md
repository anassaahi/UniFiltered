# UniFiltered - Campus Communication Redefined

UniFiltered is a modern Android application designed to bridge the communication gap on university campuses. It provides a centralized platform for students to interact and for university societies to share official news and events.

---

## 🏗 Project Architecture: MVVM
This project follows the **Model-View-ViewModel (MVVM)** architecture, which is the industry standard for Android development.

- **Model:** Represents the data and business logic (e.g., `Post`, `User`, `Comment`).
- **View:** The UI layer (Activities and Fragments) that displays data and sends user actions to the ViewModel.
- **ViewModel:** Acts as a bridge, holding the UI state and communicating with the Repository. It survives configuration changes (like rotating the screen).

---

## 🚀 Key Concepts & Implementation

### 1. Firebase (Backend as a Service)
*   **What:** A platform by Google that provides cloud services like databases and authentication.
*   **Why:** Instead of building a complex backend server, Firebase allows us to handle users and data in real-time with minimal setup.
*   **How:** 
    *   **Authentication:** Used in `AuthRepository` to register and log in users using `createUserWithEmailAndPassword`.
    *   **Firestore:** A NoSQL database used to store `users`, `posts`, and `societies`. We use "Snapshots" to listen for real-time changes.

### 2. Kotlin Coroutines & Flow
*   **What:** Coroutines are "lightweight threads" for doing background work. Flow is a stream of data that can be computed asynchronously.
*   **Why:** We don't want the app to "freeze" while fetching data from the internet. Flows allow the UI to update automatically whenever data in the database changes.
*   **How:** 
    *   **Coroutines:** Used with `viewModelScope.launch` to perform database writes.
    *   **callbackFlow:** In `PostRepository`, we convert Firebase's listener into a Flow so the UI can "collect" the latest posts.

### 3. Navigation Component
*   **What:** A library for managing all screen navigations.
*   **Why:** It simplifies the complexity of moving between Fragments and ensures the "Back" button works correctly.
*   **How:** Managed in `MainActivity` using a `NavHostFragment` and a `BottomNavigationView`. The navigation paths are defined in an XML resource file.

### 4. RecyclerView with ListAdapter
*   **What:** A UI component for displaying long lists of data efficiently.
*   **Why:** It reuses (recycles) views as you scroll, saving memory. `ListAdapter` uses `DiffUtil` to only update items that actually changed, making animations smooth.
*   **How:** Implemented in `PostAdapter.kt`. It calculates the difference between the old list and the new list to refresh the feed efficiently.

### 5. Repository Pattern
*   **What:** A class that abstracts the data source from the rest of the app.
*   **Why:** It makes the code cleaner and easier to test. The ViewModel doesn't care if the data comes from Firebase or a local cache; it just asks the Repository.
*   **How:** See `PostRepository.kt` or `AuthRepository.kt`.

### 6. Real-Time Search & Filtering
*   **What:** Filtering the list of posts as the user types.
*   **Why:** Provides an interactive and fast user experience.
*   **How:** In `FeedViewModel.kt`, we use the `combine` operator. It merges the "Search Query" and the "Post List" into a single filtered stream of data.

### 7. Network Monitoring
*   **What:** A utility to check if the device has an active internet connection.
*   **Why:** To warn users when they are offline so they don't wonder why the feed isn't updating.
*   **How:** `NetworkMonitor.kt` uses the Android `ConnectivityManager` to listen for network changes and emits the status via a Flow to `MainActivity`.

### 8. Role-Based Access Control
*   **What:** Distinguishing between "Student" and "Society" accounts.
*   **Why:** Only official societies should be able to post "Official" announcements with a blue verification tick.
*   **How:** The `User` model contains a `role` field. When creating a post, we check if the `postedAsSocietyId` is valid to apply the `isOfficialSocietyPost` badge.

---

## 🛠 Tech Stack
- **Language:** Kotlin
- **UI:** XML (ViewBinding) & Jetpack Compose (Experimental)
- **Database:** Firebase Firestore
- **Auth:** Firebase Auth
- **Image Loading:** Coil
- **Architecture:** Jetpack ViewModel, LiveData, Flow

## 📖 How to Run
1. Clone the repository.
2. Connect your Firebase project and add the `google-services.json` file to the `app/` folder.
3. Build and run on an Android device or emulator (API 24+).
