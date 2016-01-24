Android Text Manager
====================

Our goal is to make sending SMS and MMS easier on the Android OS.


Download
--------
```groovy
repositories {
  maven {
    url 'https://dl.bintray.com/bourdakos1/maven'
  }
}

dependencies {
  compile 'com.xlythe:android-text-manager:0.0.1'
}
```

Usage
-----
```java
mManager = TextManager.getInstance(context);
mManager.send(new Text.Builder()
                .message("HIII!!!!")
                .recipient("1234567890")
                .attach(bmp1)
                .attach(bmp2)
                .attach(bmp3)
                .build()
);
```

Limitations
-----------
* Re-downloading a failed MMS not yet implemented
* Dual sim support not yet added
* Demo app not yet finished


License
--------

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
