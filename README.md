Android Text Manager
====================

TextManager sends and receives SMS and MMS on Android API 21 and later. It uses WorkManager for background work and Android’s telephony service for carrier transport.


Where to Download
-----------------
```groovy
dependencies {
  implementation 'com.xlythe:android-text-manager:2.0.0'
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
Text.TextCursor texts = manager.getMessageCursor(thread);
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
<receiver android:name=".receiver.MessageReceiver" android:exported="false">
    <intent-filter>
        <action android:name="com.xlythe.textmanager.text.ACTION_TEXT_RECEIVED" />
    </intent-filter>
</receiver>
```

Permissions
-----------
The default SMS app persists incoming messages and requests the SMS role in its own package. TextManager contributes the SMS and MMS delivery receivers, quick-response service, and private PDU provider to that app’s manifest. The app supplies a composer for the SENDTO intent.

Declare and request the permissions for the features you use.

```xml
<!-- Sending SMS or MMS -->
<uses-permission android:name="android.permission.SEND_SMS" />
<!-- Receiving messages and reading the inbox -->
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.RECEIVE_MMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<!-- Optional contact names and photos -->
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

An app with SEND_SMS can also send an SMS while another app holds the role. Android persists that outgoing message. A successful send worker means Android accepted the dispatch. Carrier delivery arrives separately when a report is requested.

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
