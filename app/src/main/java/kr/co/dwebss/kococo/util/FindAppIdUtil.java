package kr.co.dwebss.kococo.util;

import android.content.Context;
import android.util.Log;
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
public class FindAppIdUtil {
    private final String TAG = "FindAppIdUtil";

    Retrofit retrofit;
    ApiService apiService;
    String APP_ID = null;

    public String getAppid(){
//        System.out.println("=======APP_ID====getAppid()=============="+APP_ID);
        return APP_ID;
    }

    public FindAppIdUtil() {
        //http 통신
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);
    }

    public String getAppid(Context context) {
        String path = context.getFilesDir().getAbsolutePath();
        //path 부분엔 파일 경로를 지정해주세요.
        File filePath = new File(path);
        if(!filePath.exists()) {
            filePath.mkdir();
        }
        //파일 유무를 확인합니다.
        File files = new File(path+"/appId.txt");
        if(files.exists()==true) {
            //파일이 있을시
            //데이터 출력하기
            FileInputStream fis = null;
            try {
                fis = context.openFileInput("appId.txt");
                BufferedReader iReader = new BufferedReader(new InputStreamReader((fis)));
                APP_ID = iReader.readLine();
                //여러줄이 있을 경우에 처리 하지만 지금은 한줄이라 안씀
                iReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            Toast.makeText(MainActivity.this, "파일이 있네유" +data,Toast.LENGTH_SHORT).show();
//            System.out.println("=======APP_ID====getAppid(Context context)=============="+APP_ID);
            return APP_ID;
        } else {
            //파일이 없을시
//            Toast.makeText(MainActivity.this, "파일이 읍네요",Toast.LENGTH_SHORT).show();
            //저장하기
            apiService.registAppid().enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    JsonObject result = response.body();
                    APP_ID = result.get("userAppId").toString().replace("\"" ,"");
                    FileOutputStream fos = null;
                    //MODE_PRIVATE 모드는 파일을 생성하여(또는 동일한 이름의 파일을 대체하여) 해당 파일을 여러분의 애플리케이션에 대해 전용으로만든다.
                    try {
                        fos = context.openFileOutput("appId.txt", Context.MODE_PRIVATE);
                        PrintWriter out = new PrintWriter(fos);
                        out.println(APP_ID);
                        out.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    //에러 날시에 다시 시작해야됨
//                    Toast.makeText(context, R.string.error_server_getRecordList,Toast.LENGTH_SHORT).show();
                    Toast.makeText(context,t.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
            return APP_ID;
        }
    }

    public void InitAppId(Context context) {
        String path = context.getFilesDir().getAbsolutePath();
        //path 부분엔 파일 경로를 지정해주세요.
        File filePath = new File(path);
        if(!filePath.exists()) {
            filePath.mkdir();
        }
        //파일 유무를 확인합니다.
        File files = new File(path+"/appId.txt");
        if(files.exists()==true) {
            //파일이 있을시
            //데이터 출력하기
            StringBuffer buffer = new StringBuffer();
            String data = null;
            FileInputStream fis = null;
            try {
                fis = context.openFileInput("appId.txt");
                BufferedReader iReader = new BufferedReader(new InputStreamReader((fis)));

                data = iReader.readLine();
                //여러줄이 있을 경우에 처리 하지만 지금은 한줄이라 안씀
//                while(data != null)
//                {
//                    buffer.append(data);
//                    data = iReader.readLine();
//                }
//                buffer.append("\n");
                iReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("=====================파일이 있네유======================"+data);

            //id가 맞는건지 검증
            apiService.getAppid(data).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(response.body()==null){
                        deleteBeforeData(context);
                        registAppid(context);
                    }else{
                        Log.v(TAG,"====아이디 검증 완료======");
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    //에러 날시에 다시 시작해야됨
//                    Toast.makeText(context, R.string.error_server_getRecordList,Toast.LENGTH_SHORT).show();
                    Toast.makeText(context,t.getMessage(),Toast.LENGTH_LONG).show();
                }
            });

        } else {
            //파일이 없을시
            registAppid(context);

        }
    }


    public void registAppid(Context context){
        //파일이 없을시
//            Toast.makeText(MainActivity.this, "파일이 읍네요",Toast.LENGTH_SHORT).show();
        //저장하기
        apiService.registAppid().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                JsonObject result = response.body();
                System.out.println("=====================파일이없어요======================"+response);
                if(result.has("userAppId")){
                    APP_ID = result.get("userAppId").toString().replace("\"" ,"");
                    FileOutputStream fos = null;
                    //MODE_PRIVATE 모드는 파일을 생성하여(또는 동일한 이름의 파일을 대체하여) 해당 파일을 여러분의 애플리케이션에 대해 전용으로만든다.
                    try {
                        fos = context.openFileOutput("appId.txt", Context.MODE_PRIVATE);
                        PrintWriter out = new PrintWriter(fos);
                        out.println(APP_ID);
                        out.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(context, "알수 없는 오류가 났습니다. 관리자에게 문의해주세요",Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                //에러 날시에 다시 시작해야됨
//                    Toast.makeText(context, R.string.error_server_getRecordList,Toast.LENGTH_SHORT).show();
                Toast.makeText(context,t.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    //초기화를 위해 이전 데이터 지움
    public void deleteBeforeData(Context context){

        String addIdPath = context.getFilesDir().getAbsolutePath()+"/appId.txt";
        deleteFile(addIdPath);

        String deletePath = context.getFilesDir().getAbsolutePath()+"/rec_data";
        deleteFile(deletePath);

    }

    public void deleteFile(String path) {
        File deleteFolder = new File(path);
        if(deleteFolder.exists()){
            if(deleteFolder.isFile()){
                deleteFolder.delete();
            }else{
                File[] deleteFolderList = deleteFolder.listFiles();
                for (int i = 0; i < deleteFolderList.length; i++) {
                    if(deleteFolderList[i].isFile()) {
                        deleteFolderList[i].delete();
                    }else {
                        deleteFile(deleteFolderList[i].getPath());
                    }
                    deleteFolderList[i].delete();
                }
            }
        }
    }




}
