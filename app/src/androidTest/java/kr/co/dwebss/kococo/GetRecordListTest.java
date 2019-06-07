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
    public void GetRecordListTest() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //테스트를 할 경우에는 로컬에서 하면 로그가 잘 안잡힘 왜인지는 모르겠는데..
        //느낌상으로는 로컬에서도 sysout을 찍어주기 때문에 로컬 sysout이 다 되기전에 안드로이드 테스트가 끝난다.
        // 이것도 사실 말이 안되는데 왜그런지는 모르겠음 로그는 안찍히는데 테스트는 잘됬대..

        //정상 id
//        String userAppId = "9eba71d5-1e49-40e2-a9b1-525e8c45aa7d";
        //에러 id
        String userAppId = "";

        System.out.println(" ========================start GetRecordListTest");
        apiService.getRecordList(userAppId,"recordId,desc").enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                System.out.println(" ========================response: "+response.body());
                System.out.println(" ========================response33333: "+response.body().toString());

                checkValueForAssertion = "success";
                assertEquals("success", checkValueForAssertion);
                System.out.println(" ========================end2 ");
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

                System.out.println(" ========================Throwable: "+ t);

                checkValueForAssertion = "fail";
                assertEquals("success", checkValueForAssertion);
                System.out.println(" ========================end3 ");
            }
        });

        System.out.println(" ========================end GetRecordListTest");
    }
}
