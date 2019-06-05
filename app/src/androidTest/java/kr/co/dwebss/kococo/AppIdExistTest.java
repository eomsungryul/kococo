package kr.co.dwebss.kococo;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import kr.co.dwebss.kococo.activity.MainActivity;
import kr.co.dwebss.kococo.http.ApiService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AppIdExistTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";

    MainActivity activity;

    @Test
    public void AppIdExistTest() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //JUnit 에서 getContext할때는 InstrumentationRegistry.getInstrumentation().getContext() 이렇겟 ㅏ용
        //안드로이드에서 테스트 되는거기 때문에 안드로이드기기에서 로그 확인가능 즉 로그필터로 볼수있음  안스에서는 안댐
        System.out.println("==========start========");
        String path = InstrumentationRegistry.getTargetContext().getFilesDir().getAbsolutePath();

        //path 부분엔 파일 경로를 지정해주세요.
        File filePath = new File(path);
        if(!filePath.exists()) {
            filePath.mkdir();
        }
        System.out.println("=================="+path);
        //파일 유무를 확인합니다.
        File files = new File(path+"/appId.txt");
        if(files.exists()==true) {
            checkValueForAssertion = "apiServiceSuccess";
        }else{
            checkValueForAssertion = "firebaseUploadFailed";
        }
        System.out.println("===========result======="+checkValueForAssertion);


        assertEquals("apiServiceSuccess", checkValueForAssertion);
    }
}
