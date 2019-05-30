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
package kr.co.dwebss.kococo.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.Record;
import kr.co.dwebss.kococo.model.RecordData;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DiaryListAdapter extends BaseAdapter {
    // Adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private ArrayList<Record> listViewItemList = new ArrayList<Record>() ;
    Boolean playBtnFlag = false;

    Retrofit retrofit;
    ApiService apiService;

    Record listViewItem;

    // ListViewAdapter의 생성자
    public DiaryListAdapter() {
    }

    // Adapter에 사용되는 데이터의 개수를 리턴. : 필수 구현
    @Override
    public int getCount() {
        return listViewItemList.size() ;
    }

    // position에 위치한 데이터를 화면에 출력하는데 사용될 View를 리턴. : 필수 구현
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.layout_diary_row, parent, false);
        }

        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
        TextView titleTextView = (TextView) convertView.findViewById(R.id.diaryNameText) ;

        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        listViewItem = listViewItemList.get(position);

        // 아이템 내 각 위젯에 데이터 반영
//        iconImageView.setImageDrawable(listViewItem.getIcon());
        titleTextView.setText("id : "+listViewItem.getRecordId()+"/"+listViewItem.getRecordStartD().replace("\"","")+"~"+listViewItem.getRecordEndD().replace("\"",""));
//        titleTextView.setText(listViewItem.getRecordStartD().replace("\"","")+"~"+listViewItem.getRecordEndD().replace("\"",""));
//        descTextView.setText(listViewItem.getDesc());


        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);


        ConstraintLayout diaryRow = (ConstraintLayout) convertView.findViewById(R.id.diaryRow) ;
        //뷰 전체를 클릭 했을 경우 페이지로 넘어간다.
        diaryRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apiService.getRecord(listViewItem.getRecordId()).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        System.out.println(" ========================response: "+response.body().toString());
                        //창 띄우기
                        Intent intent = new Intent(context, ResultActivity.class);
                        intent.putExtra("responseData",response.body().toString()); /*송신*/
                        context.startActivity(intent);
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        System.out.println(" ========================Throwable: "+ t);

                    }
                });
            }
        });

        return convertView;
    }

    // 지정한 위치(position)에 있는 데이터와 관계된 아이템(row)의 ID를 리턴. : 필수 구현
    @Override
    public long getItemId(int position) {
        return position ;
    }

    // 지정한 위치(position)에 있는 데이터 리턴 : 필수 구현
    @Override
    public Object getItem(int position) {
        return listViewItemList.get(position);
    }

    // 아이템 데이터 추가를 위한 함수. 개발자가 원하는대로 작성 가능.
    public void addItems(
            JsonArray resultDataList) {
        Record item;

        for(int i=0; i<resultDataList.size(); i++){
            JsonObject recordObj = (JsonObject) resultDataList.get(i);
            System.out.println("==================recordObj=================="+recordObj);
            item = new Record(
                    recordObj.get("recordId").getAsInt()
                    ,recordObj.get("recordStartD").toString()
                    ,recordObj.get("recordEndD").toString()
                    ,recordObj.get("recordStartDt").toString()
                    ,recordObj.get("recordEndDt").toString()
            );
            listViewItemList.add(item);
        }
    }

}
