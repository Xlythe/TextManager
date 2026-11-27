Android Text Manager
====================

TextManager sends and receives SMS and MMS on Android API 14 and later. Android handles MMS transport on API 21 and later. On API 14–20, TextManager can exchange MMS directly with the carrier over mobile data. Both paths use the same library artifact and send API.


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

SMS and MMS features
--------------------
| Feature | TextManager behavior |
| --- | --- |
| Unicode and long SMS | Android chooses the encoding and segmentation. TextManager tracks each segment and interprets GSM and CDMA delivery status reports. A temporary status leaves the send pending. |
| Flash SMS | Listen for the package-local `TextReceiver.ACTION_FLASH_SMS` broadcast and read `EXTRA_TEXT` to present the message. The library leaves saving it to the user. |
| Binary SMS | `sendDataSms(destination, port, bytes, subscriptionId, sent, delivered)` sends one datagram. To receive one, register an Android `DATA_SMS_RECEIVED` receiver for your own `sms://localhost:port` and read `SmsMessage.getUserData()` from the received PDUs. |
| MMS media | Compose group messages, Unicode subjects and captions, images, video, audio, vCards and other MIME parts. `getAttachments()` returns all the media and file parts. `getAttachment()` retains the existing single-attachment view. Use `FileAttachment` to preserve a file's original bytes and MIME type, including animated images. Recipients and carriers determine which formats and sizes they accept. |
| MMS retrieval | Decode single-part and multipart messages, including related, mixed and alternative bodies, base64 and quoted-printable parts. After successful persistence the library submits a retrieval response. |
| Delivery and read reports | `requestMmsReports(delivery, read)` requests reports from the carrier and recipient. Listen for `TextReceiver.ACTION_MMS_REPORT` with an unexported receiver in your app. Match `message_id` against `manager.getMmsDetails(sentMessage).messageId` when the carrier supplies one. The intent includes `subscription_id`, `addresses`, `date_seconds`, and the OMA `message_type` and `status` octets. A group can report separately for each recipient. |
| Sending a read report | After the user's consent, call `manager.sendMmsReadReport(message, false)` when the message is read, or pass `true` before deleting it unread. Call from a background thread. The library checks the stored request and captures the report headers before returning, so the app can delete the row immediately afterward. |

```java
manager.send("Concert tonight", new VoiceAttachment(recordingUri))
        .addAttachment(new FileAttachment(contactCardUri, "text/vcard"))
        .subject("Friday")
        .requestMmsReports(true, false)
        .usingSubscription(subscriptionId)
        .to("+14155550100", "+442079460018");
```

For explicit To, Cc and Bcc recipients, message class, priority, relative expiry, or a custom MIME presentation, build a `SendReq` and call `manager.sendMms(request, subscriptionId)` on a background thread. Both send APIs use the same persistent queue and carrier transport. Group replies omit blind recipients.

The application-facing profile covers person-to-person messaging. Full standards conformance remains unverified. Optional OMA transactions for remote MMS mailboxes, forwarding before download, replacement and cancellation have no end-to-end API here. Extended Messaging Service (EMS) objects, arbitrary SMS user-data headers and raw modem control are also outside this API. Carrier settings and the peer handset affect report availability and presentation. [3GPP SMS specification](https://www.etsi.org/deliver/etsi_ts/123000_123099/123040/15.01.00_60/ts_123040v150100p.pdf) · [OMA MMS Encapsulation](https://www.openmobilealliance.org/release/MMS/V1_3-20080128-C/OMA-TS-MMS-ENC-V1_3-20080128-C.pdf) · [Android SmsManager](https://developer.android.com/reference/android/telephony/SmsManager)

Contacts and SIM phonebooks
---------------------------
Use `getContactCursor(name)` for a contact picker and `lookupContact(address, subscriptionId)` to resolve a messaging address. Lookup checks the device directory first. On API 31 and later it also reads personal dialing records (ADN) from the selected SIM. `getSimContacts()` lists personal records across SIMs and `getSimContacts(subscriptionId)` selects one. `contact.getSimRecordUri()` identifies the subscription and record. Both directories use the app's `READ_CONTACTS` grant. Earlier Android releases can see SIM contacts that have been imported or synced into the device directory. [Android SIM phonebook](https://developer.android.com/reference/android/provider/SimPhonebookContract.SimRecords)

Lookup preserves the original dialed address. For matching national numbers it uses the SIM country, then the network country, then the device locale. International calling prefixes, trunk prefixes and significant leading zeros use libphonenumber metadata. Short codes and service codes retain their identity. Looking up a bare local number never invents a missing area code.

```java
PhoneNumbers.regionsForCallingCode(1);                // Includes US and CA
PhoneNumbers.areaCode("020 7946 0018", "GB");         // 20
PhoneNumbers.normalize("020 7946 0018", "GB");        // +442079460018
PhoneNumbers.describe("+44 20 7946 0018", "GB", Locale.UK);  // London
```

The description is an offline numbering-plan label. Mobile and non-geographic numbers may have only a country label or an empty description. [libphonenumber](https://github.com/google/libphonenumber)

Automatic MMS on API 14–20
--------------------------
The library depends on WorkManager 2.9.1 to support API 14. Check the resolved minimum SDK if your app also declares a newer WorkManager dependency.

For automatic MMS, declare the legacy network permissions in your messaging app. Include `WRITE_SMS` to save messages on API 14–18. Android grants provider write access to the default SMS app on API 19 and later.

```xml
<uses-permission android:name="android.permission.INTERNET" android:maxSdkVersion="20" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" android:maxSdkVersion="20" />
<uses-permission android:name="android.permission.READ_APN_SETTINGS" android:maxSdkVersion="16" />
<uses-permission android:name="android.permission.WRITE_SMS" android:maxSdkVersion="18" />
```

If your app needs internet on newer releases, declare its internet permission without the SDK cap. TextManager looks up the SIM operator and active MMS access point in the device’s carrier provider, then tries the device’s APN configuration file if needed. It supports an IPv4 mobile MMS route with an optional carrier proxy. Sending fails if the device cannot provide a matching configuration or route. A send completes only when the carrier acknowledges its transaction.

On API 19–20 the default SMS app automatically receives MMS pushes. On API 14–18 the stock messaging app normally downloads them. If your app takes responsibility for those downloads, override the resource below in `res/values/bools.xml`. Enabling two downloaders can duplicate messages. Incoming SMS callbacks on API 14–18 carry the broadcast content with message ID `-1` until you query the persisted inbox row.

```xml
<resources>
    <bool name="textmanager_handle_legacy_mms_push">true</bool>
</resources>
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
