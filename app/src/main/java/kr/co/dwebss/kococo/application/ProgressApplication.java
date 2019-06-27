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
package kr.co.dwebss.kococo.application;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.text.TextUtils;
import android.widget.TextView;

import kr.co.dwebss.kococo.R;


public class ProgressApplication extends Application {

    private static ProgressApplication ProgressApplication;
//    AppCompatDialog progressDialog;
    ProgressDialog progressDialog;
    public static ProgressApplication getInstance() {
        return ProgressApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ProgressApplication = this;
    }

    public void progressON(Activity activity, String message) {

        if (activity == null || activity.isFinishing()) {
            return;
        }


//        if (progressDialog != null && progressDialog.isShowing()) {
        if (progressDialog != null && progressDialog.isShowing()) {
//            progressSET(message);
        } else {

//            progressDialog = new AppCompatDialog(activity);
//            progressDialog.setCancelable(false);
//            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//            progressDialog.setContentView(R.layout.progress_loading);
//            progressDialog.show();

            progressDialog = new ProgressDialog(activity,R.style.MyTheme);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
            progressDialog.show();

        }
//        ImageView img_loading_frame = (ImageView) progressDialog.findViewById(R.id.iv_frame_loading);
//        Glide.with(progressDialog.getContext())
//             .load(R.drawable.loading_bar)
//             .into(new DrawableImageViewTarget(img_loading_frame));
//
    }

    public void progressSET(String message) {

        if (progressDialog == null || !progressDialog.isShowing()) {
            return;
        }

        TextView tv_progress_message = (TextView) progressDialog.findViewById(R.id.tv_progress_message);
        if (!TextUtils.isEmpty(message)) {
            tv_progress_message.setText(message);
        }

    }

    public void progressOFF() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

}
