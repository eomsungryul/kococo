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

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class RecordClaimTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";
    @Test
    public void recordClaim() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);
        String fileNm = "snoring-20192404_1624-04_1624_1559633052843.mp3";

        String uploadFirebasePath = "rec_data/7f6b2855-a435-46ab-b9f4-4b15ef476b8f/2019-06-04/"+fileNm;

        //FindAppIdUtil fau = new FindAppIdUtil();
        String appId =  "7f6b2855-a435-46ab-b9f4-4b15ef476b8f";

        FirebaseStorage storage = FirebaseStorage.getInstance("gs://kococo-2996f.appspot.com/");

        StorageReference storageRef = storage.getReference();
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        StorageReference spaceRef = storageRef.child(uploadFirebasePath);

        //내 실제 경로를 입력한다.
        Uri file = Uri.fromFile(new File("/data/data/kr.co.dwebss.kococo/files/rec_data/9/"+fileNm));
        UploadTask uploadTask = spaceRef.putFile(file);

        // 파일 업로드의 성공/실패에 대한 콜백 받아 핸들링 하기 위해 아래와 같이 작성한다

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                System.out.println("======================실패"+exception.getMessage());
                //Toast.makeText(getApplicationContext(),"파일 업로드에 실패하였습니다. ",Toast.LENGTH_SHORT).show();
                checkValueForAssertion = "firebaseUploadFailed";
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("======================성공");
                //신고 전송
                JsonObject requestJson = new JsonObject();
                requestJson.addProperty("analysisServerUploadPath",uploadFirebasePath);
                JsonArray analysisDetailsList = new JsonArray();
                JsonObject analysisDetailsObj = new JsonObject();
                analysisDetailsObj.addProperty("claimReasonCd",200101);
                analysisDetailsObj.addProperty("claimContents","test");
                analysisDetailsList.add(analysisDetailsObj);
                requestJson.add("analysisDetailsList",analysisDetailsList);

                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestJson));
                apiService.addClaim(153, requestData).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        System.out.println(" ========================response: "+response.body().toString());
                        //저장 시에 뒤로가기
                        //Toast.makeText(getApplicationContext(),"신고하기가 완료되었습니다.",Toast.LENGTH_LONG).show();
                        //ReportActivity.super.onBackPressed();
                        //requestClaimFlag = true;
                        checkValueForAssertion = "apiServiceSuccess";
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        System.out.println(" ========================Throwable: "+ t);
                        //Toast.makeText(getApplicationContext(),"신고하기가 실패되었습니다.",Toast.LENGTH_LONG).show();
                        checkValueForAssertion = "apiServiceFailed";
                    }
                });
            }
        });
        //String body= "{\"analysisServerUploadPath\":\"rec_data/7f6b2855-a435-46ab-b9f4-4b15ef476b8f/2019-06-03/snoring-20194603_1246-03_1249_1559533768896.mp3\",\"analysisDetailsList\":[{\"claimReasonCd\":200101,\"claimContents\":\"recordData : /storage/emulated/0/Download/rec_data/28/snoring-20194603_1246-03_1249_1559533768896.mp3/getAnalysisDetailsId :154/getAnalysisId :135\"}]}";

        assertEquals("apiServiceSuccess", checkValueForAssertion);
    }
}
