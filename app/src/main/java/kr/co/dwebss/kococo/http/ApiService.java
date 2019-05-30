package kr.co.dwebss.kococo.http;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.Map;

import kr.co.dwebss.kococo.model.ApiCode;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    //API url
//    public static final String API_URL = "http://ec2-52-79-240-67.ap-northeast-2.compute.amazonaws.com:8080/";
    public static final String API_URL = "http://52.79.88.47:8080/";

    @GET("kococo/api/code")
    Call<ApiCode> getApiCode();

    //json으로도 받을 수 있는 형태
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @GET("kococo/api/code")
    Call<JsonObject> getApiCode2();

    @POST("kococo/api/userappid")
    Call<JsonObject> getAppid();


    //녹음 종료시 저장
    //json으로도 받을 수 있는 형태
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @POST("kococo/api/record")
    Call<JsonObject> addRecord(@Body RequestBody data);

    //신고하기 제출
    //json으로도 받을 수 있는 형태
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @PUT("kococo/api/claim/analysisDetail/{analysisDetailId}")
    Call<JsonObject> addClaim(@Path("analysisDetailId") Integer analysisDetailId, @Body RequestBody data);

    //@Query 를 이용하면 ?key=value 식으로 보내진다.
    @GET("kococo/api/record/search/findByUserAppId")
    Call<JsonObject> getRecordList(@Query("userAppId") String userAppId,@Query("sort") String sort);

    //@Path 를 이용하면 Rest 방식으로 호출 할 수 있다.
    @GET("kococo/api/record/{recordId}")
    Call<JsonObject> getRecord(@Path("recordId") Integer recordId);

//    Call<JSONObject> addRecord(@Body JSONObject data);
//    Call<JsonObject> addRecord(JsonObject data);

//    @POST("api/users")
//    Call<User> createUser(@Body User user);
//
//    @GET("api/users?")
//    Call<UserList> doGetUserList(@Query("page") String page);
//
//    @FormUrlEncoded
//    @POST("api/users?")
//    Call<UserList> doCreateUserWithField(@Field("name") String name, @Field("job") String job);
}
