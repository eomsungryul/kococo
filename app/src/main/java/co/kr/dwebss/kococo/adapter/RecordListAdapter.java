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
package co.kr.dwebss.kococo.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import co.kr.dwebss.kococo.R;
import co.kr.dwebss.kococo.activity.ReportActivity;
import co.kr.dwebss.kococo.model.RecodeData;

public class RecordListAdapter extends BaseAdapter {
    // Adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private ArrayList<RecodeData> listViewItemList = new ArrayList<RecodeData>() ;
    Boolean playBtnFlag = false;

    // ListViewAdapter의 생성자
    public RecordListAdapter() {
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
            convertView = inflater.inflate(R.layout.layout_record_row, parent, false);
        }

        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
//        ImageView iconImageView = (ImageView) convertView.findViewById(R.id.imageView1) ;
        TextView titleTextView = (TextView) convertView.findViewById(R.id.recordNameText) ;
//        TextView descTextView = (TextView) convertView.findViewById(R.id.textView2) ;

        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        RecodeData listViewItem = listViewItemList.get(position);

        // 아이템 내 각 위젯에 데이터 반영
//        iconImageView.setImageDrawable(listViewItem.getIcon());
        titleTextView.setText(listViewItem.getRowName());
//        descTextView.setText(listViewItem.getDesc());

        playBtnFlag = false;
        //lisrViews내의 아이콘 버튼 참조 및 onclick추가
        ImageButton playBtn = (ImageButton) convertView.findViewById(R.id.recordPlay);
        playBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "ㅎㅇ : "+position+" : "+getItem(position), Toast.LENGTH_SHORT).show();
                //재생버튼 누를 시 정지버튼으로 변경하는 메소드
                if(!playBtnFlag){
                    playBtn.setImageResource(R.drawable.baseline_pause_white_48dp);
                    playBtnFlag = true;
                }else{
                    playBtn.setImageResource(R.drawable.baseline_play_arrow_white_48dp);
                    playBtnFlag = false;
                }
            }
        });

        ImageButton reportBtn = (ImageButton) convertView.findViewById(R.id.recordReport);
        reportBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //창 띄우기 startActivity를 이용하여 안에 intent를 생성
                //intent (Context,class)
                Intent i = new Intent(context, ReportActivity.class);
                //intent에 값을 넣어야 해당 값을 신고하기 페이지에서 가져올수있다.
                i.putExtra("testData",getItem(position).toString());
                //신고하기 창 열기
                v.getContext().startActivity(i);
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
        return listViewItemList.get(position).getRowName();
    }

    // 아이템 데이터 추가를 위한 함수. 개발자가 원하는대로 작성 가능.
    public void addItem(
//            Drawable icon, String title,
            String name) {
        RecodeData item = new RecodeData(name,"1");
        listViewItemList.add(item);
    }
}
