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

package com.emmanuelkehinde.twittasave.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.emmanuelkehinde.twittasave.R
import com.emmanuelkehinde.twittasave.utils.TWITTER_PROFILE_LINK
import com.google.android.material.appbar.MaterialToolbar

class AboutActivity : AppCompatActivity() {

    private val linkTextView: TextView by lazy { findViewById(R.id.txt_link) }
    private val toolbar: MaterialToolbar by lazy { findViewById(R.id.toolbar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = "About"
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        linkTextView.setOnClickListener {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(TWITTER_PROFILE_LINK)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(urlIntent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
