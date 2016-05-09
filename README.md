Android Text Manager
====================

Our goal is to make sending SMS and MMS easier on the Android OS.


Where to Download
-----------------
```groovy
dependencies {
  compile 'com.xlythe:android-text-manager:0.0.3'
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
To get a list of conversations aka "threads"
```java
List<Thread> threads = manager.getThreads().get();

// You can also get a cursor of threads
Thread.ThreadCursor threads = manager.getThreadCursor();
```

To get a list of messages
```java
List<Text> texts = manager.getMessages(thread).get();

// You can also get a cursor of messages
Text.TextCursor texts = manager.getMessageCursor(thread)
```

With a thread you can get the latest message
```java
Text text = thread.getLatestMessage(context).get();

// With a text you can get more info
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

Receiving and Storing Messages
------------------------------
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
Lastly, but very import, don't forget to add permissions to the manifest
```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.SEND_MMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.RECEIVE_MMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.WRITE_SMS" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.WRITE_CONTACTS" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_PROFILE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<!-- You may not need all of these, depending on what you are doing -->
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
