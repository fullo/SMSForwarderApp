# SMSForwarder

An Android application that automatically forwards incoming SMS messages to your email address. The app runs as a background service to ensure continuous SMS monitoring and forwarding.

## Features

- **Automatic SMS Forwarding**: Instantly forwards incoming SMS messages to your specified email address
- **Background Service**: Runs continuously in the background with foreground service for reliable operation
- **Customizable Templates**: Configure custom email subject and body formats with placeholders
- **Comprehensive Logging**: View detailed logs of all forwarded messages with success/failure status
- **Message Retry**: Manually retry failed message forwards from the log interface
- **Permission Management**: Proper handling of SMS and notification permissions
- **Material Design UI**: Clean, modern interface following Material Design guidelines

## Requirements

### Gmail Account Required
This application uses **Google's SMTP server** (currently hardcoded) for sending emails. You **must have a Gmail account** to use this application.

### App-Specific Password Recommended
For security reasons, it's **strongly recommended** to create an app-specific password for this application rather than using your main Gmail password:

1. Go to your [Google Account settings](https://myaccount.google.com/)
2. Navigate to Security → 2-Step Verification
3. Under "App passwords", generate a new app-specific password
4. Use this password in the SMSForwarder settings instead of your regular Gmail password

### Android Requirements
- **Compile SDK**: Android 14 (API level 34)
- **Minimum SDK**: Android 9 (API level 28)
- **Target SDK**: Android 13 (API level 33)
- **Permissions Required**:
  - `RECEIVE_SMS`: To intercept incoming SMS messages
  - `INTERNET`: To send emails via SMTP
  - `FOREGROUND_SERVICE`: To run background service reliably
  - `POST_NOTIFICATIONS`: For service status notifications (Android 13+)

> **📱 Device Recommendation**: This application is optimized to work on older spare mobile devices. Using bleeding-edge Android versions is **not recommended** as the app is designed for reliability and compatibility with older hardware that users typically repurpose for SMS forwarding.

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/fullo/SMSForwarderApp.git
   ```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Build and install the APK on your Android device:
   ```bash
   ./gradlew assembleDebug
   ```

## Setup and Configuration

### Initial Setup
1. **Grant Permissions**: On first launch, grant SMS and notification permissions when prompted
2. **Configure Email Settings**: 
   - Tap "Settings" to open the configuration screen
   - Enter your Gmail address
   - Enter your app-specific password (recommended) or Gmail password
   - Enter the recipient email address (can be the same as sender)
   - Optionally customize the email subject and body templates

### Template Customization
You can customize the email format using these placeholders:
- `{sender}`: Phone number of SMS sender
- `{time}`: Timestamp when SMS was received
- `{body}`: Content of the SMS message

**Default Templates:**
- Subject: `SMS from {sender}`
- Body: `From: {sender}\nTime: {time}\n\n{body}`

### Starting the Service
1. After configuration, return to the main screen
2. Tap "Start Service" to begin SMS monitoring
3. The app will run in the background with a persistent notification

## Project Structure

```
app/src/main/java/com/apropos/smsforwarder/
├── MainActivity.kt              # Main UI and permission handling
├── SettingsActivity.kt          # Email configuration interface
├── LogActivity.kt               # SMS forwarding history viewer
├── SmsForwarderService.kt       # Background service for SMS monitoring
├── SmsReceiver.kt               # BroadcastReceiver for SMS interception
├── EmailSender.kt               # SMTP email sending functionality
├── SmsLog.kt                    # Database operations for logging
├── SmsLogEntry.kt               # Data model for log entries
├── SMSForwarderApp.kt           # Application class
├── ServiceRestartWorker.kt      # WorkManager for service reliability
└── ui/theme/
    ├── Color.kt                 # Material Design color definitions
    ├── Theme.kt                 # Application theme configuration
    └── Type.kt                  # Typography definitions

app/src/main/res/
├── layout/
│   ├── activity_main.xml        # Main screen layout
│   ├── activity_settings.xml    # Settings screen layout
│   ├── activity_log.xml         # Log viewer layout
│   └── log_item.xml             # Individual log entry layout
├── values/
│   ├── strings.xml              # String resources
│   ├── colors.xml               # Color definitions
│   └── themes.xml               # Theme styles
├── drawable/
│   ├── ic_notification.xml      # Notification icon
│   ├── ic_sms_forwarder.xml     # App icon
│   ├── ic_launcher_background.xml
│   └── ic_launcher_foreground.xml
└── xml/
    ├── backup_rules.xml         # Backup configuration
    └── data_extraction_rules.xml
```

### Key Components

**MainActivity**: Main entry point handling UI, permissions, and service control
**SmsReceiver**: Intercepts incoming SMS messages and triggers email forwarding
**EmailSender**: Handles SMTP connection to Gmail and email composition
**SmsForwarderService**: Foreground service ensuring continuous operation
**SmsLog**: Room database implementation for storing forwarding history

## Technical Details

### Email Implementation
- Uses **Gmail SMTP** (`smtp.gmail.com:587`) - **hardcoded**
- Supports **STARTTLS** encryption for secure transmission
- Implements **JavaMail API** (android-mail 1.6.7) for email sending
- SMTP authentication required with Gmail credentials
- Currently hardcoded to Gmail (future versions may support other providers)

### Database
- **Room Database** (version 2.5.2) for local storage of SMS forwarding logs
- Database name: `sms_log_database` with version 2
- **Automatic schema migration** with destructive fallback
- Stores sender phone number, message content, timestamp, and delivery status
- DAO operations: getAll, insert, updateSentStatus, deleteAll

### Background Processing
- **Foreground Service** for reliable background operation with persistent notification
- **WorkManager** (version 2.8.1) integration for service restart capability
- **Kotlin Coroutines** for asynchronous email sending operations
- **START_STICKY** service mode for automatic restart after system kills
- Proper **battery optimization** handling with notification channel management

### Security Considerations
- Email credentials stored in **SharedPreferences** (unencrypted)
- SMS content temporarily stored in local Room database
- Future versions will implement credential encryption with Android Keystore

### Key Dependencies
- **AndroidX Core**: 1.10.1
- **Material Components**: 1.9.0 for UI design
- **JavaMail**: android-mail 1.6.7 and android-activation 1.6.7
- **Room Database**: 2.5.2 with Kotlin extensions
- **WorkManager**: 2.8.1 for background task management
- **Kotlin Coroutines**: 1.6.4 for async operations
- **Jetpack Compose**: 2023.06.01 BOM (for future UI components)
- **RecyclerView**: 1.3.1 for log display

## Usage

1. **First Time Setup**: Configure email settings and grant permissions
2. **Start Service**: Enable the SMS forwarding service
3. **Monitor Status**: Check the persistent notification for service status
4. **View Logs**: Access forwarding history and retry failed messages
5. **Manage Service**: Stop/start the service as needed

## Troubleshooting

### Common Issues

**Service Not Starting**
- Verify all permissions are granted
- Ensure email configuration is complete
- Check that Gmail credentials are correct

**Emails Not Sending**
- Verify internet connection
- Check Gmail app-specific password
- Ensure Gmail account has SMTP access enabled
- Review logs for specific error messages

**Missing SMS**
- Confirm app is set as default SMS handler (if required)
- Check battery optimization settings
- Verify SMS permissions are granted

## Future Enhancements

- [ ] Support for additional email providers (Outlook, Yahoo, custom SMTP)
- [ ] End-to-end encryption for stored credentials
- [ ] Message filtering and rules engine
- [ ] MMS forwarding support
- [ ] Multiple recipient support
- [ ] Backup and restore functionality
- [ ] Widget for quick service toggle

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Disclaimer

This application is designed for personal use. Ensure you comply with local laws and regulations regarding SMS interception and forwarding. The developers are not responsible for any misuse of this application.

## Support

For issues and questions:
- Create an issue on GitHub
- Check existing issues for similar problems
- Provide detailed logs when reporting bugs

---

**Note**: This application requires a Gmail account with app-specific password for optimal security. Standard Gmail passwords may work but are not recommended for security reasons.
