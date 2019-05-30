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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ReportActivity;
import kr.co.dwebss.kococo.model.RecordData;
import kr.co.dwebss.kococo.util.MediaPlayerUtility;

public class RecordListAdapter extends BaseAdapter {

    // Adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private ArrayList<RecordData> listViewItemList = new ArrayList<RecordData>() ;
    Boolean playBtnFlag = false;
    ImageButton playBtn;

    //재생할때 필요한
    MediaPlayer mediaPlayer;

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
        RecordData listViewItem = listViewItemList.get(position);

        // 아이템 내 각 위젯에 데이터 반영
//        iconImageView.setImageDrawable(listViewItem.getIcon());
        titleTextView.setText(listViewItem.getTitle());
//        descTextView.setText(listViewItem.getDesc());

        playBtnFlag = false;
        //lisrViews내의 아이콘 버튼 참조 및 onclick추가
        playBtn = (ImageButton) convertView.findViewById(R.id.recordPlay);
        playBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "ㅎㅇ : "+position+" : "+listViewItem.getAnalysisFileAppPath(), Toast.LENGTH_SHORT).show();
                //재생버튼 누를 시 정지버튼으로 변경하는 메소드
                if(!playBtnFlag){
                    //재생일 시에
                    playBtn.setImageResource(R.drawable.baseline_pause_white_48dp);
                    playBtnFlag = true;
                    SimpleDateFormat stringtoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    long startTerm = 0L;
                    long endTerm = 0L;
                    try {
                        Date analysisStartDt =  stringtoDateFormat.parse(listViewItem.getAnalysisStartDt().toString());

                        Date termStartDt =  stringtoDateFormat.parse(listViewItem.getTermStartDt().toString());
                        Date termEndDt =  stringtoDateFormat.parse(listViewItem.getTermEndDt().toString());
                        startTerm = termStartDt.getTime()-analysisStartDt.getTime();
                        endTerm = termEndDt.getTime()-termStartDt.getTime();
//                        System.out.println("================끝나자:"+listViewItem.getTermStartDt().toString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    play((int)startTerm,(int)endTerm,listViewItem.getAnalysisFileAppPath()+listViewItem.getAnalysisFileNm(),context);

                }else{
                    playBtn.setImageResource(R.drawable.baseline_play_arrow_white_48dp);
                    playBtnFlag = false;
                    stopPlayer();
                }
            }
        });

        //신고하기 버튼
        //신고하기를 클릭 할 시에 데이터를 보낸다!
        ImageButton reportBtn = (ImageButton) convertView.findViewById(R.id.recordReport);
        reportBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //창 띄우기 startActivity를 이용하여 안에 intent를 생성
                //intent (Context,class)
                Intent i = new Intent(context, ReportActivity.class);
                //intent에 값을 넣어야 해당 값을 신고하기 페이지에서 가져올수있다.
                i.putExtra("testData", listViewItem);
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
        return listViewItemList.get(position);
    }

    // 아이템 데이터 추가를 위한 함수. 개발자가 원하는대로 작성 가능.
    public void addItem(
    //            Drawable icon, String title,
    RecordData date) {
        listViewItemList.add(date);
    }

    public void play(int startTime, int endTime , String filePath, Context context){
        mediaPlayer = MediaPlayer.create(context, Uri.parse("/data/data/kr.co.dwebss.kococo/files/rec_data/23/snoring-20191029_0410-29_0410_1559113854914.wav"));
//        mediaPlayer = MediaPlayer.create(context, Uri.parse(filePath));
//        mediaPlayer = MediaPlayer.create(context, R.raw.queen);
        //구간 재생
        mediaPlayer.seekTo(startTime);
        mediaPlayer.getCurrentPosition();
        mediaPlayer.start();
        //카운트 다운
        new CountDownTimer(endTime, 100) {
            public void onTick(long millisUntilFinished) {
                if(MediaPlayerUtility.getTime(mediaPlayer)>=endTime){
                    mediaPlayer.stop();
                    // 초기화
                    mediaPlayer.reset();
//                    testFlag = false;
//                    testBtn.setText("시작");
                }
            }
            public void onFinish() {
                playBtn.setImageResource(R.drawable.baseline_play_arrow_white_48dp);
                playBtnFlag = false;
            }
        }.start();
    }
///data/user/0/kr.co.dwebss.kococo/files/rec_data/23event-20191029_0410-29_0410_1559113854925.wav

    public void stopPlayer(){
        mediaPlayer.stop();
        mediaPlayer.reset();
    }



}
