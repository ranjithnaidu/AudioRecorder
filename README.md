# AudioRecorder

Audio recorder with playback using MVVM architecture pattern

This project demonstrates the app architecture of an Audio recorder and playback on android.
 
##### What's the app about?

The app allows users to record an audio sample for 20secs and allows the user to click on the play button once the recording is finished. Opens a bottom sheet with the Audio playback.

Please find the Screeshots below:

![Screenshot_1561398648](https://user-images.githubusercontent.com/2275562/60041491-2645c400-96ee-11e9-8c5d-7894090297c7.png)
![Screenshot_1561398658](https://user-images.githubusercontent.com/2275562/60041492-26de5a80-96ee-11e9-845e-43ece9ed560e.png)
![Screenshot_1561398674](https://user-images.githubusercontent.com/2275562/60041493-2776f100-96ee-11e9-8ee0-0a456efa0609.png)

## App Architecture

This app is entirely written in Java.

[MVVM](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) has been used in the app to keep the business logic away from the activities and fragments. [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) and [Data Binding](https://developer.android.com/topic/libraries/data-binding) was used as required to bind the views and data elements.

The job of service is to interact with the Media Recorder apis and record an audio sample for the Playback later.

Junit and mokito library has been used for Unit-testing.

For any queries please contact <ranjithnaidu.v@gmail.com>
