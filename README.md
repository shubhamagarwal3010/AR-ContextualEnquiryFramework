## OnBoarding AR App

Tw Onboarding app is an augmented reality application. It is basically composed of two building blocks:
1. A Vuforia ar portal, where the user can couple target images with metadata file, which stores video / image / hyperlink url in json format.
2. an Android application, that can recognize such target images once acquired by the device camera, and render the corresponding content from metadata in overlap. 

### How does it work?

1. User just publishes a new image target with the respective url of image/video/hyperlink in textual metadata file
2. The Vuforia SDK living in the Mobile Application queries the cloud web services about the presence of a target image in the acquired frame;
3. If a target is present, its metadata is sent to the mobile application, containing the respective media URL;
4. Application renders respective Image / Video associated with the url in metadata upon the target image within the acquired frame.

### Getting started
Here's how it works:
1. Create a Vuforia developer account [here](https://developer.vuforia.com/license-manager).
2. In your Target Manager, create a cloud database and load your first target image, also providing a text file as metadata, containing the media type and url of the media content you want to couple in json.
3. Checkout this repository “https://git.thoughtworks.net/ar-onboarding/tw-onboarding” in a local folder.
4. In your Vuforia Developer Portal, find out your license key, client access keys for your database, and add them to your Bash Profile like this

Open terminal and give this command

`$ vi ~/.bash_profile // on Mac if you are using Bash`

and add the following lines to it 

```
export VUFORIA_LICENCE_KEY="replace-with-licence-key"
export VUFORIA_ACCESS_KEY="replace-with-access-key"
export VUFORIA_SECRET_KEY="replace-with-secret-key"
```

Now, give the following command

`$ source ~/.bash_profile`

Done, you are all set to go!


Open the project now with [Android Studio](https://developer.android.com/studio/index.html) and have fun!

In case you launched the project in Android Studio before setting these env variables, it might not update them dynamically. So, restart the IDE and it should work fine.
