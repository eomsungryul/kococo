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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ProgressApplication;
import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.activity.UserConsultDetailActivity;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.Record;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserConsultListAdapter extends BaseAdapter {
    // Adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private ArrayList<Record> listViewItemList = new ArrayList<Record>() ;
    Boolean playBtnFlag = false;

    Retrofit retrofit;
    ApiService apiService;

    Record listViewItem;
    ProgressApplication pa;

    // ListViewAdapter의 생성자
    public UserConsultListAdapter() {
        pa = new ProgressApplication();

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
            convertView = inflater.inflate(R.layout.layout_user_consult_row, parent, false);
        }

        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
        TextView titleTextView = (TextView) convertView.findViewById(R.id.consultNameText) ;

        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        listViewItem = listViewItemList.get(position);

        // 아이템 내 각 위젯에 데이터 반영
//        iconImageView.setImageDrawable(listViewItem.getIcon());
//        titleTextView.setText("id : "+listViewItem.getRecordId()+"/"+listViewItem.getRecordStartD().replace("\"","")+"~"+listViewItem.getRecordEndD().replace("\"",""));
        if('N'==listViewItem.getConsultingReplyYn()){
            titleTextView.setText(listViewItem.getRecordStartD().replace("\"","")+"~"+listViewItem.getRecordEndD().replace("\"","")
            +" [답변 대기]"
            );
        }else{
            titleTextView.setText(listViewItem.getRecordStartD().replace("\"","")+"~"+listViewItem.getRecordEndD().replace("\"","")
            +" [답변 완료]"
            );
        }
//        descTextView.setText(listViewItem.getDesc());


        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);


        ConstraintLayout consultRow = (ConstraintLayout) convertView.findViewById(R.id.consultRow) ;
        //뷰 전체를 클릭 했을 경우 페이지로 넘어간다.
        consultRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                pa.progressON((Activity)v.getContext(),"");
                getConsultDetail(context,position);
            }
        });

        return convertView;
    }


    private void getConsultDetail(Context context,int position) {
        Record data = (Record) getItem(position);
//        Toast.makeText(context,"진행중",Toast.LENGTH_LONG).show();
        apiService.getRecord(data.getRecordId()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                System.out.println(data.getRecordId()+" ========================response: "+response.body());
                //창 띄우기
                Intent intent = new Intent(context, UserConsultDetailActivity.class);
                intent.putExtra("responseData",response.body().toString()); /*송신*/
                context.startActivity(intent);
                pa.progressOFF();
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                System.out.println(" ========================Throwable: "+ t);
            }
        });
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

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    // 아이템 데이터 추가를 위한 함수. 개발자가 원하는대로 작성 가능.
    public void addItems(
            JsonArray resultDataList) {
        Record item;
        if(resultDataList.size()>0){
            for(int i=0; i<resultDataList.size(); i++){
                JsonObject recordObj = (JsonObject) resultDataList.get(i);
//                System.out.println("=====consult=======addItems======recordObj=================="+recordObj);
                item = new Record(
                        recordObj.get("recordId").getAsInt()
                        ,recordObj.get("recordStartD").getAsString()
                        ,recordObj.get("recordEndD").getAsString()
                        ,recordObj.get("recordStartDt").getAsString()
                        ,recordObj.get("recordEndDt").getAsString()
                        ,recordObj.get("consultingReplyYn").getAsCharacter()
                );
                listViewItemList.add(item);
            }
        }
    }
}
