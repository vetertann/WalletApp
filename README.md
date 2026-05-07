# Wallet

Android app for tracking and planning expenses by parsing Alta Bank SMS messages from `Alta_Banka`.

## Features

- Imports SMS card payment notifications from the device inbox
- Parses amount, currency, merchant, timestamp, and available balance
- Lets you assign categories to transactions
- Learns merchant rules so future similar SMS messages are auto-categorized
- Shows monthly totals and simple bar-chart reports
- Stores data locally with Room

## Notes

- The app requests `READ_SMS` and `RECEIVE_SMS` because it reads bank messages directly from the inbox and can refresh when new SMS messages arrive.
- This is suitable for personal sideloaded use. SMS permissions are tightly restricted for Play Store distribution.
- Open the folder in Android Studio and let Gradle sync. The local Android SDK at `~/Library/Android/sdk` already contains platform levels through API 35.
