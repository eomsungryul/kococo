package kr.co.dwebss.kococo;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import kr.co.dwebss.kococo.activity.ReportActivity;
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
public class AddClaimTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";

    @Test
    public void AddClaimTest() throws Exception {
        // 파이어베이스에서 에러가 날 수 있는 요인들
        // 1. mp3 파일이 없는 경우
        // 2. path가 이상할 경우
        // 3. 아이디가 맞지않는 경우
        // 4.
        System.out.println("==============AddClaimTest======== start");

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

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        String uploadFirebasePath = "rec_data/"+appId+"/"+sdf.format(date)+"/"+fileNm;

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("analysisServerUploadPath",uploadFirebasePath);
        JsonArray analysisDetailsList = new JsonArray();
        JsonObject analysisDetailsObj = new JsonObject();
        analysisDetailsObj.addProperty("claimReasonCd",100201);
        analysisDetailsObj.addProperty("claimContents", "ddddd");
        analysisDetailsList.add(analysisDetailsObj);
        requestJson.add("analysisDetailsList",analysisDetailsList);

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestJson));
        System.out.println(" =============ddd===========requestJson: "+ requestJson.toString());
        System.out.println(" ============eeee============requestData: "+ requestData.toString());

//        {"analysisServerUploadPath":"rec_data/7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f/2019-05-30/snoring-20191330_1513-30_1514_1559196886407.wav","analysisDetailsList":[{"claimReasonCd":200102,"claimContents":"recordData : /data/user/0/kr.co.dwebss.kococo/files/rec_data/29/snoring-20191330_1513-30_1514_1559196886407.wav/getAnalysisDetailsId :97/getAnalysisId :93"}]}
//        {"userAppId":"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f","recordStartDt":"2019-05-30T18:54:48","recordEndDt":"2019-05-30T18:55:09","analysisList":[{"analysisStartDt":"2019-05-30T18:54:56","analysisEndDt":"2019-05-30T18:55:16","analysisFileAppPath":"/storage/emulated/0/Download/rec_data/1","analysisFileNm":"snoring-20190530_1854-30_1855_1559210109319.mp3","analysisDetailsList":[]}]}

        apiService.addClaim(AnalysisDetailsId,requestData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                System.out.println(" ==============AddClaimTest==========response: "+response.body());
                JsonObject jsonObject = response.body();
                if(jsonObject==null){
                    System.out.println("============AddClaimTest==========실패");
                    checkValueForAssertion = "fail";
                    assertEquals("success", checkValueForAssertion);
                }else{
                    System.out.println("============AddClaimTest==========성공");
                    //저장 시에 뒤로가기
                    checkValueForAssertion = "success";
                    assertEquals("success", checkValueForAssertion);
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                System.out.println("============AddClaimTest==========실패");
                System.out.println(" ========================Throwable: "+ t);
                checkValueForAssertion = "fail";
                assertEquals("success", checkValueForAssertion);
            }
        });
        //An unknown error occurred, please check the HTTP result code and inner exception for server response.

        //비동기 테스트는 어렵기 때문에 쓰레드를 잠시 멈추게 하던가 Awaitility라는 라이브러리를 사용하면된다고함
        //출처 : https://stackoverflow.com/questions/631598/how-to-use-junit-to-test-asynchronous-processes
        Thread.sleep(5000);

        System.out.println("==============AddClaimTest======== end");
    }
}
