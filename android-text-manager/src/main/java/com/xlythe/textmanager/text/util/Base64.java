/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xlythe.textmanager.text.util;

public class Base64 {
    public static int NO_WRAP = android.util.Base64.NO_WRAP;

    /** Decode MIME base64, including folded lines and unpadded final groups. */
    public static byte[] decodeBase64(byte[] data) {
        if (data == null) return null;
        try {
            return android.util.Base64.decode(data, android.util.Base64.DEFAULT);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    public static String encodeToString(byte[] data, int flags) {
        return android.util.Base64.encodeToString(data, flags);
    }
}
