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
package co.kr.dwebss.kococo.activity;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import co.kr.dwebss.kococo.R;

public class ReportActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출

        setContentView(R.layout.activity_report);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;
        super.onCreate(savedInstanceState);

        TextView test  = findViewById(R.id.declareTxtHeader);
        Bundle extras = getIntent().getExtras();
        String extra = extras.getString("testData");
        test.setText(extra);

        ImageButton bt = (ImageButton) findViewById(R.id.previousButton);
        bt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReportActivity.super.onBackPressed();
            }
        });

        Button declareBtn = (Button) findViewById(R.id.declareBtn);
        declareBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeclareDialog();
            }
        });

    }

    //얼럿 다이얼로그 띄우기
    void showDeclareDialog()
    {
        //Style을 넣어서 커스텀 가능
        // 타이틀이 없으면 안나오고 메세지 길이에 따라 경고창길이가 달라진다.
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.MyAlertDialogStyle);
        builder.setTitle("제출하기");
        builder.setMessage("제출하시겠습니까?");
        //setView()를 이용하여 view를 넣고 커스텀 할 수 있다.
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(),"예를 선택했습니다.",Toast.LENGTH_LONG).show();
                    }
                });
        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(),"아니오를 선택했습니다.", Toast.LENGTH_LONG).show();
                    }
                });
        builder.show();
        //이런식으로 높이와 길이를 지정할수있지만 비율에 맞게 버튼위치가 늘어나지않음 비추.
//        builder.show().getWindow().setLayout(600, 400);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged " + newConfig.screenWidthDp + "," + newConfig.screenHeightDp);
    }

}
