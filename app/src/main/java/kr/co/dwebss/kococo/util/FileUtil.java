package kr.co.dwebss.kococo.util;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.JsonObject;

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

/*
 * 오디오 분석 모듈 코골이
 *
 * */
public class FileUtil {

    Retrofit retrofit;
    ApiService apiService;
    String APP_ID = null;
    public FileUtil() {
        //http 통신
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);
    }

    public String removeFiles(String filePath) {
        File f = new File(filePath);
        if(f.delete()){
            return "정상적으로 삭제되었습니다.";
        }else{
            return "이미 삭제된 파일입니다.";
        }
    }

}
