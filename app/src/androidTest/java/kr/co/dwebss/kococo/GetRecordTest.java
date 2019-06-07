package kr.co.dwebss.kococo;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

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
public class GetRecordTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";
    @Test
    public void recordClaim() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);
        //FindAppIdUtil fau = new FindAppIdUtil();
        //정상 id
        int recordId =  193;
        //에러 id
//        int recordId =  200;


        System.out.println(" ========================start ");

        apiService.getRecord(recordId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                System.out.println(" ========================response: "+response.body().toString());
                System.out.println(" ========================response2222222222222: "+response.body().toString());
                checkValueForAssertion = "success";
                assertEquals("success", checkValueForAssertion);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                System.out.println(" ========================Throwable: "+ t);
                checkValueForAssertion = "fail";
                assertEquals("success", checkValueForAssertion);
            }
        });
        System.out.println(" ========================end ");
    }
}
