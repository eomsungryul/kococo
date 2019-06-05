package kr.co.dwebss.kococo;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import kr.co.dwebss.kococo.activity.MainActivity;
import kr.co.dwebss.kococo.http.ApiService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AppIdSelectTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";
    String APP_ID = null;

    @Test
    public void AppIdSelectTest() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //JUnit 에서 getContext할때는 InstrumentationRegistry.getTargetContext() 을 이용
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
            FileInputStream fis = null;
            try {
                fis = InstrumentationRegistry.getTargetContext().openFileInput("appId.txt");
                BufferedReader iReader = new BufferedReader(new InputStreamReader((fis)));
                APP_ID = iReader.readLine();
                //여러줄이 있을 경우에 처리 하지만 지금은 한줄이라 안씀
                iReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(APP_ID!=null){
                checkValueForAssertion = "success";
            }else{
                checkValueForAssertion = "fail";
            }
        }else{
            checkValueForAssertion = "not found";
        }
        System.out.println("===========APP_ID======="+APP_ID);
        System.out.println("===========result======="+checkValueForAssertion);

        assertEquals("success", checkValueForAssertion);
    }
}
