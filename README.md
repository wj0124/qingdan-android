# LiShiManager Android

Android project scaffold for the LiShiManager iOS app.

Status: native Android implementation in progress. The core iOS workflows are now mirrored with Jetpack Compose and Material 3 UI.

The iOS reference app is a local-first SwiftUI app with:

- An `Item` model: `name`, `quantity`, `sortIndex`.
- Two main pages: registration and summary.
- Item management, shop information, receipt-style export, and photo saving.

Implemented Android workflows:

- Item management: add, rename, delete, sort, select all, batch delete, batch import.
- Count page: increment, decrement, direct quantity input, reset all quantities.
- Summary page: receipt-style list, rename receipt title, copy text, save receipt image to Photos/Pictures.
- Shop information: shop name, contact, phone, address.
- Local-only persistence with SharedPreferences JSON.

The UI is implemented with Jetpack Compose, including Android Studio previews for the main count and summary screens.
