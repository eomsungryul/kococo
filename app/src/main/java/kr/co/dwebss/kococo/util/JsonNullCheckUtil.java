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
 * Gson JsonObject 널 체크 관련 유틸
 *
 * */
public class JsonNullCheckUtil {

    public JsonNullCheckUtil() {

    }

    public static String JsonStringNullCheck(JsonObject obj, String key){
        if(obj.get(key)==null){
            return "";
        }else{
            return obj.get(key).toString().replace("\"","");
        }
    }

}
