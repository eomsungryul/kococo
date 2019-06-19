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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ReportActivity;
import kr.co.dwebss.kococo.model.RecordData;
import kr.co.dwebss.kococo.util.MediaPlayerUtility;

public class RecordListAdapter extends BaseAdapter {

    private String LOG_TAG = "RecordListAdapter";

    // Adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private ArrayList<RecordData> listViewItemList = new ArrayList<RecordData>() ;
    Boolean playBtnFlag = false;
    Boolean isPlaying = false;
    int playPosition;
    Context context;
    ViewGroup viewGroup;

    //재생할때 필요한
    MediaPlayer mediaPlayer = new MediaPlayer();
    CountDownTimer cdt;

    int analysisId;

    private GraphClickListener graphClickListener;
    int pos = 0;
    // ListViewAdapter의 생성자
    public RecordListAdapter(Context context, GraphClickListener graphClickListener) {
        this.context = context;
        this.graphClickListener = graphClickListener;
    }



    public interface GraphClickListener{
        void clickBtn(RecordData listViewItem,Boolean playFlag);
    }

    // Adapter에 사용되는 데이터의 개수를 리턴. : 필수 구현
    @Override
    public int getCount() {
        return listViewItemList.size() ;
    }

    // position에 위치한 데이터를 화면에 출력하는데 사용될 View를 리턴. : 필수 구현
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        pos = position;
        final Context context = parent.getContext();
        viewGroup = parent;

        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.layout_record_row, parent, false);
        }

        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
//        ImageView iconImageView = (ImageView) convertView.findViewById(R.id.imageView1) ;
        TextView timeTextView = (TextView) convertView.findViewById(R.id.recordTimeText) ;
//        TextView nameTextView = (TextView) convertView.findViewById(R.id.recordNameText) ;
//        TextView descTextView = (TextView) convertView.findViewById(R.id.textView2) ;

        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        RecordData listViewItem = listViewItemList.get(position);

        // 아이템 내 각 위젯에 데이터 반영
//        iconImageView.setImageDrawable(listViewItem.getIcon());
        timeTextView.setText(listViewItem.getTitle());
//        nameTextView.setText(title[0]);
//        System.out.println("===========listViewItem.getTitle()==========="+listViewItem.getTitle());
//        descTextView.setText(listViewItem.getDesc());

        playBtnFlag = false;
        //lisrViews내의 아이콘 버튼 참조 및 onclick추가
        ImageButton playBtn = (ImageButton) convertView.findViewById(R.id.recordPlay);

        System.out.println("===================filePath = :"+listViewItem.getAnalysisFileAppPath()+"/"+listViewItem.getAnalysisFileNm());

        playBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                rowClickEvt(position,listViewItem,playBtn);
            }
        });

        timeTextView.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                rowClickEvt(position,listViewItem,playBtn);
            }
        });
//
//        nameTextView.setOnClickListener(new TextView.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                rowClickEvt(position,listViewItem,playBtn);
//            }
//        });
        //{"userAppId":"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f","recordId":116,"recordStartD":"2019-05-30","recordStartDt":"2019-05-30T15:13:46","recordEndD":"2019-05-30","recordEndDt":"2019-05-30T15:14:46","consultingYn":"N","consultingReplyYn":"N","analysisList":[{"analysisId":93,"analysisStartD":"2019-05-30T00:00:00","analysisStartDt":"2019-05-30T15:13:50","analysisEndD":"2019-05-30T00:00:00","analysisEndDt":"2019-05-30T15:14:49","analysisFileNm":"snoring-20191330_1513-30_1514_1559196886407.wav","analysisFileAppPath":"/data/user/0/kr.co.dwebss.kococo/files/rec_data/29","analysisServerUploadYn":"N","analysisDetailsList":[{"analysisId":93,"analysisId":97,"termTypeCd":200102,"termStartDt":"2019-05-30T15:14:37","termEndDt":"2019-05-30T15:14:42","claimYn":"N"}],"_links":{"record":{"href":"http://52.79.88.47:8080/kococo/api/record/116"}}}],"_links":{"self":{"href":"http://52.79.88.47:8080/kococo/api/record/116"},"record":{"href":"http://52.79.88.47:8080/kococo/api/record/116"},"admin":{"href":"http://52.79.88.47:8080/kococo/api/record/116/admin"},"user":{"href":"http://52.79.88.47:8080/kococo/api/record/116/user"},"sleepStatusCd":{"href":"http://52.79.88.47:8080/kococo/api/record/116/sleepStatusCd"}}}

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
    RecordData data) {
        listViewItemList.add(data);
    }

    public void playMp(int startTime, int endTime , String filePath, Context context,ImageButton playBtn) throws IOException {
//        mediaPlayer = MediaPlayer.create(context, Uri.parse("/data/data/kr.co.dwebss.kococo/files/rec_data/23/snoring-20191029_0410-29_0410_1559113854914.wav"));
//        mediaPlayer = MediaPlayer.create(context, R.raw.queen);
        Log.v(LOG_TAG,( "======== play() startTime : " +startTime+" /endTime : "+endTime+"/filePath : "+filePath));

        File file = new File(filePath);
        if(file.exists()){
            //만일 지금 재생중이라면 멈추고 버튼도 재생버튼으로 바꾼다
                mediaPlayer = MediaPlayer.create(context, Uri.parse(filePath));
                //구간 재생
                mediaPlayer.seekTo(startTime);
                mediaPlayer.getCurrentPosition();
                mediaPlayer.start();

                //구간 재생을 위한 카운트 다운 타이머
                cdt = new CountDownTimer(endTime-startTime, 100) {
                    public void onTick(long millisUntilFinished) {
//                        System.out.println("=============================onTick"+millisUntilFinished);
                        if(mediaPlayer==null){
                            stopMp(playBtn);
                        }else if(MediaPlayerUtility.getTime(mediaPlayer)>=endTime){
                            stopMp(playBtn);
                        }
                        if(!isPlaying){
//                            System.out.println("============cancel=================onTick");
                            cancel();
                        }
                    }
                    public void onFinish() {
                        stopMp(playBtn);
                    }
                }.start();
            isPlaying=true;
        }else{
            Toast.makeText(context,"파일이 존재하지않습니다.",Toast.LENGTH_SHORT).show();
            playBtn.setImageResource(R.drawable.baseline_play_arrow_white_48dp);
            playBtnFlag = false;
        }
    }

    public void stopMp(ImageButton playBtn){
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        playBtn.setImageResource(R.drawable.baseline_play_arrow_white_48dp);
        playBtnFlag = false;
        isPlaying=false;
    }



    public void playActivityMp(int adi,int startTime, int endTime , String filePath, Context context)throws IOException {
//        View v = viewGroup.getChildAt(position);
//        ImageButton playBtn = (ImageButton) v.findViewById(R.id.recordPlay);
        View v;
        ImageButton playBtn;
        if(isPlaying && analysisId != adi){
            //재생 중지 버튼
            for(int i=0; i<getCount();i++){
                View v2 = viewGroup.getChildAt(i);
                ImageButton playBtn2 = (ImageButton) v2.findViewById(R.id.recordPlay);
                playBtn2.setImageResource(R.drawable.baseline_play_arrow_white_48dp);
            }
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
            cdt.cancel();
            playBtnFlag = false;
            isPlaying = false;
        }
        System.out.println("==========123====test============"+getCount()+viewGroup.getChildCount());
        for(int i=0; i<getCount();i++){
            v = viewGroup.getChildAt(i);
            RecordData rd =listViewItemList.get(i);

            TextView timeTextView = (TextView) v.findViewById(R.id.recordTimeText) ;
            System.out.println("========123======timeTextView.getText()============"+timeTextView.getText());

            System.out.println("=======123======rd.getanalysisId()======="+rd.getAnalysisId()+"====position=="+adi);
            if(rd.getAnalysisId()==adi){
                playBtn = (ImageButton) v.findViewById(R.id.recordPlay);
                playBtnFlag = true;
                analysisId = adi;
                playBtn.setImageResource(R.drawable.baseline_pause_white_48dp);
                playMp(startTime, endTime , filePath, context, playBtn);
                break;
            }
        }
    }

    public void stopActivityMp(int position){
        View v;
        ImageButton playBtn;
        for(int i=0; i<getCount();i++){
            v = viewGroup.getChildAt(i);
            RecordData rd =listViewItemList.get(i);
            if(rd.getAnalysisId()==position){
                playBtn = (ImageButton) v.findViewById(R.id.recordPlay);
                playBtnFlag = true;
                stopMp(playBtn);
                break;
            }
        }
    }

    public void playGraphMp(int termStartDt, int termEndDt, String filePath, Context applicationContext) throws IOException {
        System.out.println("=============termStartDt()======="+termStartDt+"====termEndDt=="+termEndDt);
//        playMp(termStartDt, termEndDt , filePath, applicationContext, playBtn);
    }

    public void rowClickEvt(int position,RecordData listViewItem,ImageButton playBtn) {
//        System.out.println("===============파일getResponseObj정보 : "+position+" : "+listViewItem.getResponseObj()+"/"+listViewItem.getAnalysisFileNm());
        //후에 액티비티에 접근할때 사용한다.
        graphClickListener.clickBtn(listViewItem,playBtnFlag);
        //재생버튼 누를 시 정지버튼으로 변경하는 메소드
        //재생중이거나 해당버튼이 재생position 값과 다른 경우에만 초기화를 해줌
        if(isPlaying && playPosition != position){
            //재생 중지 버튼
            for(int i=0; i<getCount();i++){
                View v2 = viewGroup.getChildAt(i);
                ImageButton playBtn2 = (ImageButton) v2.findViewById(R.id.recordPlay);
                playBtn2.setImageResource(R.drawable.baseline_play_arrow_white_48dp);
            }
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
            cdt.cancel();
            playBtnFlag = false;
            isPlaying = false;
        }
        if(!playBtnFlag){
            //재생일 시에
            playBtn.setImageResource(R.drawable.baseline_pause_white_48dp);
            playBtnFlag = true;
            playPosition = position;
//            System.out.println("=============position==============="+playPosition);
            SimpleDateFormat stringtoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            long startTerm = 0L;
            long endTerm = 0L;
            try {
                //재생구간만 하는것
                Date analysisStartDt =  stringtoDateFormat.parse(listViewItem.getAnalysisStartDt().toString());
                Date analysisEndDt =  stringtoDateFormat.parse(listViewItem.getAnalysisEndDt().toString());
//                Date termStartDt =  stringtoDateFormat.parse(listViewItem.getTermStartDt().toString());
//                Date termEndDt =  stringtoDateFormat.parse(listViewItem.getTermEndDt().toString());
//                startTerm = termStartDt.getTime()-analysisStartDt.getTime();
//                endTerm = termEndDt.getTime()-analysisStartDt.getTime();
                //전체구간 재생
                startTerm = 0;
                endTerm = analysisEndDt.getTime()-analysisStartDt.getTime();
//                        System.out.println("================termEndDt:"+termEndDt);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                playMp((int)startTerm,(int)endTerm,listViewItem.getAnalysisFileAppPath()+"/"+listViewItem.getAnalysisFileNm(),context,playBtn);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            stopMp(playBtn);
        }
    }
    public boolean getPlayBtnFlag(){
        return playBtnFlag;
    }

    public void destroyMp() {
        if(cdt!=null){
            cdt.cancel();
        }
        mediaPlayer.release();
        mediaPlayer=null;
    }



}