/*
 * Copyright (C) 2017 Emmanuel Kehinde
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.emmanuelkehinde.twittasave.utils

import android.Manifest

/**
 * Created by kehinde on 3/31/17.
 */

const val NOTIFICATION_IDENTIFIER = 3100
const val STOP_AUTO_LISTEN_REQUEST_CODE = 20
const val OPEN_ACTIVITY_REQUEST_CODE = 30
const val EXTRA_AUTO_LISTEN_SERVICE_ON = "auto_listen_service_on"
const val TWITTER_PROFILE_LINK = "https://twitter.com/emmakoko96"

object Constant {
    // Storage Permissions
    const val REQUEST_EXTERNAL_STORAGE = 1
    val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
}
