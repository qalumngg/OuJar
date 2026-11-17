# Our Jar: setup

A couples app for Android: love notes, a shared mood marble jar, a date calendar, an anniversary counter and important dates, plus two home screen widgets (love note and mood jar).

Everything is built by GitHub, so no computer is needed. These steps can all be done from a phone browser.

## 1. Create the Firebase project (one time)

1. Go to https://console.firebase.google.com and create a project (any name, turn off Google Analytics).
2. Click the Android icon to add an app.
   - Android package name: `com.qalumn.ourjar`
   - Nickname: Our Jar. Skip the SHA-1 field.
3. Download `google-services.json` when it offers it.

## 2. Turn on sign-in and the database

1. Build > Authentication > Get started > Sign-in method > **Anonymous** > Enable > Save.
2. Build > Firestore Database > Create database.
   - Location: `australia-southeast1` (Sydney) is closest to Brisbane.
   - Start in **production mode**.
3. In Firestore, open the **Rules** tab, replace everything with the contents of `firestore.rules` from this repo, and press **Publish**.

## 3. Add google-services.json to the repo

Put the downloaded file in the `app/` folder of the repo, so it ends up at `app/google-services.json`.

## 4. Build and install

Every push to `main` builds the app. When the build finishes (about 5 minutes), open the repo's **Releases** page, download `OurJar.apk` on both phones and install it. Android will ask you to allow installs from your browser the first time.

## 5. First run

1. One of you taps **Start our space**, enters a name and, if you like, your anniversary.
2. Send the code that appears to your partner.
3. Your partner taps **Join my partner**, enters the code, and adds their name.

To add the widgets: press and hold the home screen, tap Widgets, find **Our Jar**.

## Notes

- Widgets refresh whenever the app is opened and about every 15 minutes in the background. Android does not allow faster than that without a paid push service.
- Widgets keep a light cream look in both light and dark mode, like a paper note.
- `app/ourjar.jks` is the signing key. It is kept in the repo so every build is signed the same way and updates install over the old version. Keep this repo private.
