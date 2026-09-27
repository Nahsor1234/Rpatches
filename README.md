# Rpatches

Custom Android patches built for use with Morphe.

## Current patch

- **Disable Motion screenshot protection**
- Target: `com.elearning.motion` 4.2.17
- Changes the app-specific `Window.setFlags(FLAG_SECURE, FLAG_SECURE)` call to `Window.clearFlags(FLAG_SECURE)`.

The GitHub Actions workflow builds the `.mpp` bundle using the current Morphe Patches template.
