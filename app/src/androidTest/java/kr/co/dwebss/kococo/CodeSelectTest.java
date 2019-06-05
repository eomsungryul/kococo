package kr.co.dwebss.kococo;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import kr.co.dwebss.kococo.http.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class CodeSelectTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";

    @Test
    public void CodeSelectTest() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //JUnit 에서 getContext할때는 InstrumentationRegistry.getTargetContext() 이렇겟 ㅏ용
        //안드로이드에서 테스트 되는거기 때문에 안드로이드기기에서 로그 확인가능 즉 로그필터로 볼수있음  안스에서는 안댐
        System.out.println("==========start========");
        String path = InstrumentationRegistry.getTargetContext().getFilesDir().getAbsolutePath();

        apiService.getApiCode().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonObject result = response.body();
                System.out.println(" ============getApiCode2============result: "+result);
                checkValueForAssertion = "success";
                System.out.println("===========result======="+checkValueForAssertion);

                assertEquals("success", checkValueForAssertion);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                checkValueForAssertion = "fail";
                System.out.println("===========result======="+checkValueForAssertion);

                assertEquals("success", checkValueForAssertion);

            }
        });
    }
}
