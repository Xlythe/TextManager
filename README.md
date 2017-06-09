Android Text Manager
====================

Our goal is to make sending SMS and MMS easier on the Android OS.


Where to Download
-----------------
```groovy
dependencies {
  compile 'com.xlythe:android-text-manager:1.0.0'
}
```

Getting Started
---------------
First thing to do is grab an instance of TextManager
```java
TextManager manager = TextManager.getInstance(context);
```

Reading Messages
----------------
You can get conversation threads either as a list, or as a cursor
```java
List<Thread> threads = manager.getThreads().get();
```
```java
Thread.ThreadCursor threads = manager.getThreadCursor();
```

And the same is true for texts
```java
List<Text> texts = manager.getMessages(thread).get();
```
```java
Text.TextCursor texts = manager.getMessageCursor(thread)
```

Texts have information on the sender, content, and timestamp.
```java
text.getSender();
text.getBody();
text.getAttachment();
text.getTimestamp();
```

Sending Messages
----------------
To send a message:
```java
manager.send("Hello World").to("1234567890");
manager.send(new ImageAttachment(uri)).to("1234567890", "9998881234", "1112223456"...);
manager.send("Hello World", new VideoAttachment(uri)).to(contact);
```

To reply to a thread or message (This handles group messaging):
```java
manager.send("Hello World").to(thread);
manager.send("Hello World").to(text);
```

Receiving Messages
------------------
Just extend our TextReceiver
```java
public class MessageReceiver extends TextReceiver {
    @Override
    public void onMessageReceived(Context context, Text text) {
    
    }
}
```
And add the receiver to your manifest
```xml
<receiver android:name=".receiver.MessageReceiver">
    <intent-filter>
        <action android:name="com.xlythe.textmanager.text.ACTION_TEXT_RECEIVED" />
    </intent-filter>
</receiver>
```

Permissions
-----------
Finally, here are the permissions you'll need to use the library.
```xml
<!-- You may not need all of these, depending on what you are doing -->

<!-- Used to send and receive messages -->
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.WRITE_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.SEND_MMS" />
<uses-permission android:name="android.permission.RECEIVE_MMS" />

<!-- Used to get contact information to send messages -->
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.WRITE_CONTACTS" />

<!-- Used to get your information to know which messages are yours -->
<uses-permission android:name="android.permission.READ_PROFILE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!-- Mms uses data network to send -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
<uses-permission android:name="android.permission.INTERNET" />

<!-- Keeps the phone awake while downloading messages -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

License
-------

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
