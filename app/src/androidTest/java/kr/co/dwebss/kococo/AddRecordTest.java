package kr.co.dwebss.kococo;

import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;

import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.http.ApiService;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AddRecordTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";

    @Test
    public void AddRecordTest() throws Exception {
        // 파이어베이스에서 에러가 날 수 있는 요인들
        // 1. mp3 파일이 없는 경우
        // 2. path가 이상할 경우
        // 3. 아이디가 맞지않는 경우
        // 4.
        System.out.println("==============AddRecordTest======== start");

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //형태
        // 아래 값중 값이 하나라도 빠져있으면, 400 bad request 발생
        //{
        //  "analysisServerUploadPath" : "/storage/rec_data",
        //  "analysisDetailsList" : [ {
        //    "claimReasonCd" : 100101, //100201-'코골이가아닙니다.', 100202-'기타'
        //    "claimContents" : "테스트"
        //  } ]
        //}

        String fileNm = "snoring-20190607_1002-07_1003_1559869391912.mp3";
        String appId =  "9eba71d5-1e49-40e2-a9b1-525e8c45aa7d";
        //정상 데이터
        int AnalysisDetailsId = 161;
        //이상한 데이터
//        int AnalysisDetailsId = 112903;
        int claimReasonCd;

        SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String recordStartDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
        String recordEndDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));

        JsonArray ansList = new JsonArray();
        JsonObject recordData = new JsonObject();

        JsonObject ans = new JsonObject();
        ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(System.currentTimeMillis())));
        ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(System.currentTimeMillis())));
        ans.addProperty("analysisFileAppPath","awdawdawd");
        ans.addProperty("analysisFileNm","awdawdadawdad");

        JsonArray ansDList = new JsonArray();
        JsonObject ansd = new JsonObject();
        ansd.addProperty("termTypeCd",200103);
        ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date(System.currentTimeMillis())));
        ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date(System.currentTimeMillis())));
        ansDList.add(ansd);
        ans.add("analysisDetailsList", ansDList);
        ansList.add(ans);

        recordData.addProperty("userAppId",appId);
        recordData.addProperty("recordStartDt",recordStartDt);
        recordData.addProperty("recordEndDt",recordEndDt);
        recordData.add("analysisList", ansList);



        RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(recordData));
        System.out.println(" ================녹음 종료 시 DB 저장========requestData: "+requestData.toString());

        apiService.addRecord(requestData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                System.out.println(" ==============AddRecordTest==========response: "+response.body());
                JsonObject jsonObject = response.body();
                if(jsonObject==null){
                    System.out.println("============AddRecordTest==========실패");
                    checkValueForAssertion = "fail";
                    assertEquals("success", checkValueForAssertion);
                }else{
                    System.out.println("============AddRecordTest==========성공");
                    //저장 시에 뒤로가기
                    checkValueForAssertion = "success";
                    assertEquals("success", checkValueForAssertion);
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                System.out.println("============AddRecordTest==========실패");
                System.out.println(" ========================Throwable: "+ t);
                checkValueForAssertion = "fail";
                assertEquals("success", checkValueForAssertion);
            }
        });
        //An unknown error occurred, please check the HTTP result code and inner exception for server response.

        //비동기 테스트는 어렵기 때문에 쓰레드를 잠시 멈추게 하던가 Awaitility라는 라이브러리를 사용하면된다고함
        //출처 : https://stackoverflow.com/questions/631598/how-to-use-junit-to-test-asynchronous-processes
        Thread.sleep(5000);

        System.out.println("==============AddRecordTest======== end");
    }
}
