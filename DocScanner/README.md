# Doc Scanner (Android)

1. Install Android Studio (Koala or newer), choose File > Open, select this folder.
2. Let Gradle sync, plug in a phone (or start an emulator) and press Run.
3. First scan needs internet once: it downloads Arabic + English OCR data (~20 MB). After that it works offline.

Use: Camera or Upload image > check/correct the fields > Save > Export Excel (share/save the .xlsx).

## Get an APK without Android Studio
Upload this folder's contents to a new GitHub repository. Open the Actions tab > "Build APK" > Run workflow.
When it finishes (about 5 minutes), download the "DocScanner-apk" artifact, unzip it, and install app-debug.apk on your phone
(allow "Install unknown apps" when asked).
