package kr.co.dwebss.kococo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import kr.co.dwebss.kococo.http.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AudioAuthExistTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";

    @Test
    public void AudioAuthExistTest() {
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //JUnit 에서 getContext할때는 InstrumentationRegistry.getTargetContext() 이렇겟 ㅏ용
        //안드로이드에서 테스트 되는거기 때문에 안드로이드기기에서 로그 확인가능 즉 로그필터로 볼수있음  안스에서는 안댐
        System.out.println("==========start========");
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};
        int permissionReadStorage = ContextCompat.checkSelfPermission(InstrumentationRegistry.getTargetContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionWriteStorage = ContextCompat.checkSelfPermission(InstrumentationRegistry.getTargetContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionAudio = ContextCompat.checkSelfPermission(InstrumentationRegistry.getTargetContext(), Manifest.permission.RECORD_AUDIO);

        if(permissionReadStorage == PackageManager.PERMISSION_DENIED || permissionWriteStorage == PackageManager.PERMISSION_DENIED||permissionAudio == PackageManager.PERMISSION_DENIED) {
            checkValueForAssertion ="fail";
        } else {
            checkValueForAssertion ="success";
        }


        System.out.println("==========PackageManager.PERMISSION_DENIED========"+PackageManager.PERMISSION_DENIED);
        System.out.println("==========permissionReadStorage========"+permissionReadStorage);
        System.out.println("==========permissionWriteStorage========"+permissionWriteStorage);
        System.out.println("==========permissionAudio========"+permissionAudio);
        System.out.println("==========result========"+checkValueForAssertion);
        System.out.println("==========end========");
        assertEquals("success", checkValueForAssertion);
    }
}
