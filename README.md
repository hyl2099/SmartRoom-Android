# Smart Room



# doorbell
The Android Things Doorbell sample demonstrates how to create a “smart” doorbell.
The sample captures a button press from a user, obtains an image via a camera peripheral,
processes the image data using Google’s Cloud Vision API, and uploads the image, Cloud Vision
annotations and metadata to a Firebase database where it can be viewed by a companion app.

## How camera works

![Smart Room][demo-gif]


## Schematics

![Schematics](schematics-all.png)

## Pre-requisites

- Android Things compatible board
- Android Things compatible camera (for example, the Raspberry Pi 3 camera module)
- Android Studio 2.2+
- Google Cloud project with Cloud Vision API enabled
- Firebase project with Database and Storage
- The following individual components:
    - 1 push button
    - 1 resistor
    - jumper wires
    - 1 breadboard

## Setup and Build

To setup, follow these steps below.

1.  Add a valid Google Cloud Vision API key in the constant `CloudVisionUtils.CLOUD_VISION_API_KEY`
  - Create a Google Cloud Platform (GCP) project on [GCP Console](https://console.cloud.google.com/)
  - Enable Cloud Vision API under Library
  - Add an API key under Credentials
  - Copy and paste the Cloud Vision API key to the constant in `CloudVisionUtils.java`

2.  Add a valid `google-services.json` from Firebase to `app/` and
    `companionApp/`
  - Create a Firebase project on [Firebase Console](https://console.firebase.google.com)
  - Add an Android app with your specific package name in the project
  - Download the auto-generated `google-services.json` and save to `app/` and `companionApp/` folders

3.  Ensure the security rules for your Firebase project allow public read/write
    access. **Note:** The rules in this section are set to public read/write for
    demonstration purposes only.
  - Firebase -> Database -> Rules:

          {
            "rules": {
              ".read": true,
              ".write": true
            }
          }

  - Firebase -> Storage -> Rules:

          service firebase.storage {
            match /b/{bucket}/o {
              match /{allPaths=**} {
                allow read, write;
              }
            }
          }


There are two modules: `app` and `companionApp`, the former is on device while the latter on
companion device e.g. Android phone.

## Running

To run the `app` module on an Android Things board:

1. Connect a push button to your device's GPIO pin according to the schematics below
2. Deploy and run the `app` module
3. Take a picture by pushing the button
4. Verify from Firebase Console that pictures are uploaded to a log in the Firebase database
   of your project
5. Verify from Firebase Console that the uploaded pictures in the log get annotations after
   a small delay from the GCP Cloud Vision

To run the `companionApp` module on your Android phone:

1. Deploy and run the `companionApp` module
2. Verify that you see a new annotated picture every time you push the button

## Enable auto-launch behavior

This sample app is currently configured to launch only when deployed from your
development machine. To enable the main activity to launch automatically on boot,
add the following `intent-filter` to the app's manifest file:

```xml
<activity ...>

    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.HOME"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>

</activity>
```



[demo-gif]: camera.gif
