package kr.co.dwebss.kococo;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import kr.co.dwebss.kococo.http.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AppIdCreateTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";
    String APP_ID = null;

    @Test
    public void AppIdSelectTest() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //JUnit 에서 getContext할때는 InstrumentationRegistry.getTargetContext() 이렇겟 ㅏ용
        //안드로이드에서 테스트 되는거기 때문에 안드로이드기기에서 로그 확인가능 즉 로그필터로 볼수있음  안스에서는 안댐
        System.out.println("==========start========");
        String path = InstrumentationRegistry.getTargetContext().getFilesDir().getAbsolutePath();

        //path 부분엔 파일 경로를 지정해주세요.
        File filePath = new File(path);
        if(!filePath.exists()) {
            filePath.mkdir();
        }
        System.out.println("=================="+path);

        apiService.getAppid().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonObject result = response.body();
                APP_ID = result.get("userAppId").toString().replace("\"" ,"");
                checkValueForAssertion = "success";
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                //에러 날시에 다시 시작해야됨
                checkValueForAssertion = "fail";
            }
        });

        System.out.println("===========APP_ID======="+APP_ID);
        System.out.println("===========result======="+checkValueForAssertion);

        assertEquals("success", checkValueForAssertion);
    }
}
