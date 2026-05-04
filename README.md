# EcoAdmin Movile

Android client for the EcoAdmin hazardous-waste transfer platform.

This repository starts the mobile version of the existing backend at `C:/Users/afp5/Git/servidor_api/servidor_api/ServidorApiRest` and is intentionally aligned with the current server contract instead of inventing a separate API.

## Current Scope

This first mobile slice includes:

- Session-based sign-in against the existing Spring Security form login.
- Runtime-configurable backend URL, with `http://10.0.2.2:8080/` as the emulator default.
- Dashboard metrics from `/api/estadisticas`.
- Transfers list and basic transfer detail from `/api/traslados`.
- Centers list from `/api/centros`.
- Authenticated profile snapshot from `/api/perfil`.

## Technical Direction

The app follows a pragmatic Kotlin Android setup:

- Kotlin + Jetpack Compose + Material 3.
- ViewModel-driven UI state.
- Repository layer over Retrofit + OkHttp.
- Session persistence through a dedicated `JSESSIONID` cookie jar.
- Runtime host switching through an interceptor, so the base server URL can be changed from the login screen without rebuilding the app.

This is intentionally a thin mobile client over the existing backend rules.

## Backend Integration Notes

- Authentication is form login, not JWT.
- The mobile client posts to `/login` with `application/x-www-form-urlencoded` fields.
- The backend uses `JSESSIONID`, so the app persists the session cookie locally.
- `/api/**` is already exempt from CSRF in the backend security config, which makes the current session approach viable for Android.
- CORS is not a blocker for the native app. It only matters for browser clients.

## Local Setup

1. Open the project in Android Studio.
2. Use JDK 17 for Android Gradle builds.
3. Start the backend from `ServidorApiRest` on port `8080`.
4. Run the Android emulator or connect a device.
5. Sign in with the same credentials you use in the web application.

If you run on a physical device, replace the default backend URL with the LAN IP of the machine hosting the Spring Boot server.

## Project Structure

```text
app/src/main/java/com/ecoadminmovile/
├── app/                 # App-wide session state
├── core/                # Preferences, network, models, factories
├── data/                # Repositories over the backend API
├── feature/auth/        # Login flow
├── feature/dashboard/   # KPI overview
├── feature/transfers/   # Transfers list and detail
├── feature/centers/     # Centers list
└── feature/profile/     # Authenticated user snapshot
```

## What Is Not Implemented Yet

The mobile app still needs later phases for:

- Residues and pick-ups.
- Transfer status transitions and history timeline.
- PDF downloads, QR workflows, and map flows.
- Attachments, photos, and offline support.
- Automated tests and Gradle wrapper generation on a machine with a full Android toolchain.

See [docs/mobile-roadmap.md](docs/mobile-roadmap.md) for the phase mapping.
