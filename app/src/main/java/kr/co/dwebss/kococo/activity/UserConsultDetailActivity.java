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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.RecordData;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserConsultDetailActivity extends AppCompatActivity {

    private static final String TAG = "UserConsultDetail";

    JsonObject responseData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출

        setContentView(R.layout.activity_user_consult_detail);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;
        super.onCreate(savedInstanceState);

        ImageButton bt = (ImageButton) findViewById(R.id.previousButton);
        bt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserConsultDetailActivity.super.onBackPressed();
            }
        });

        TextView answerTxt = (TextView) findViewById(R.id.answer_txt);
        EditText answerVal = (EditText) findViewById(R.id.answer_val);
        answerTxt.setVisibility(View.INVISIBLE);
        answerVal.setVisibility(View.INVISIBLE);

        //데이터 수신
        Intent intent = getIntent();
        if(getIntent().hasExtra("responseData")){
            responseData = new JsonParser().parse(getIntent().getStringExtra("responseData")).getAsJsonObject();

            EditText consultTitle = (EditText)findViewById(R.id.title_val);
            consultTitle.setText(responseData.get("consultingTitle").getAsString());
            consultTitle.setEnabled(false);
            EditText consultContents = (EditText)findViewById(R.id.contents_val);
            consultContents.setText(responseData.get("consultingContents").getAsString());
            //enable false but allow scrolling
            consultContents.setKeyListener(null);
            consultContents.setFocusable( false );
            consultContents.setCursorVisible(false);


            if(responseData.has("consultingReplyContents")){
                answerVal.setText(responseData.get("consultingReplyContents").getAsString());
                answerTxt.setVisibility(View.VISIBLE);
                answerVal.setVisibility(View.VISIBLE);
                answerVal.setEnabled(false);
            }

        }else{
            Toast.makeText(this,"알수 없는 에러가 발생하였습니다. 뒤로 이동합니다.",Toast.LENGTH_SHORT).show();
            UserConsultDetailActivity.super.onBackPressed();
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged " + newConfig.screenWidthDp + "," + newConfig.screenHeightDp);
    }

}
