/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.dwebss.kococo.activity;

import android.app.Activity;

import kr.co.dwebss.kococo.application.ProgressApplication;


public class ProgressActivity extends Activity {

    public void progressON() {
        ProgressApplication.getInstance().progressON(this, null);
    }

    public void progressON(String message) {
        ProgressApplication.getInstance().progressON(this, message);
    }

    public void progressOFF() {
        ProgressApplication.getInstance().progressOFF();
    }
}
