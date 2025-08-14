# SMSForwarder Project Summary

## Overview
SMSForwarder is an Android application that automatically forwards incoming SMS messages to a specified email address. It runs as a background service and provides a user interface for configuration and log viewing.

## Key Features
1. Automatic SMS forwarding to email
2. Background service for continuous operation
3. User interface for configuration
4. Log viewing capability
5. Service start/stop functionality

## Project Structure

### Main Components
1. `MainActivity`: The main entry point of the application, handling UI and user interactions.
2. `SmsForwarderService`: A foreground service that listens for incoming SMS messages.
3. `SmsReceiver`: A BroadcastReceiver that intercepts incoming SMS messages.
4. `LogActivity`: An activity to display the forwarding logs.
5. `SmsLog`: A data class and associated database operations for storing log entries.

### UI Components
- `activity_main.xml`: Layout for the main configuration screen.
- `activity_log.xml`: Layout for the log viewing screen.

### Database
- Room database for storing log entries.

### Theme
- Jetpack Compose theme components for consistent UI styling.

## Key Files

1. `app/build.gradle.kts`: Contains project dependencies and Android configuration.
2. `app/src/main/AndroidManifest.xml`: Declares app components and permissions.
3. `app/src/main/java/com/apropos/smsforwarder/MainActivity.kt`: Main activity code.
4. `app/src/main/java/com/apropos/smsforwarder/SmsForwarderService.kt`: Background service code.
5. `app/src/main/java/com/apropos/smsforwarder/SmsReceiver.kt`: SMS interception code.
6. `app/src/main/java/com/apropos/smsforwarder/LogActivity.kt`: Log viewing activity code.
7. `app/src/main/java/com/apropos/smsforwarder/SmsLog.kt`: Log data and database operations.
8. `app/src/main/java/com/apropos/smsforwarder/ui/theme/`: Jetpack Compose theme files.

## Setup Instructions
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Build and run the application on an Android device or emulator.

## Testing (tbd)
The project includes both unit tests and instrumented tests:
- Unit tests: Located in `app/src/test/java/com/apropos/smsforwarder/`
- Instrumented tests: Located in `app/src/androidTest/java/com/apropos/smsforwarder/`

To run tests:
- For unit tests: `./gradlew test`
- For instrumented tests: `./gradlew connectedAndroidTest`

## Permissions
The app requires the following permissions:
- `RECEIVE_SMS`: To intercept incoming SMS messages.
- `INTERNET`: To send emails.
- `FOREGROUND_SERVICE`: To run as a foreground service.
- `NOTIFICATION`: To forcibly enable notifications. 

## Notes for Developers
- Ensure proper error handling and user notifications for failed forwarding attempts.
- Follow Android best practices for background services and battery optimization.

## Future Enhancements
1. Implement message filtering options.
2. Add support for MMS forwarding.
4. Create a widget for quick service toggling.
5. Add support for multiple email recipients.
