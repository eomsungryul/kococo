package kr.co.dwebss.kococo;

import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import kr.co.dwebss.kococo.http.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class GetRecordListTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";
    @Test
    public void recordClaim() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //정상 id
        String userAppId = "9eba71d5-1e49-40e2-a9b1-525e8c45aa7d";
        //에러 id
//        String userAppId = "";

        System.out.println(" ========================start ");

        apiService.getRecordList(userAppId,"recordId,desc").enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                System.out.println(" ========================response: "+response.body());
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
