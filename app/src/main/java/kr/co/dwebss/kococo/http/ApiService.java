package kr.co.dwebss.kococo.http;

import com.google.gson.JsonObject;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    //API url
    //회사 내컴
//    public static final String API_URL = "http://192.168.0.2:8080/";
    //aws 주소
    public static final String API_URL = "http://52.79.88.47:8080/kococo/";

    //json으로도 받을 수 있는 형태
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @GET("api/code")
    Call<JsonObject> getApiCode();

    @POST("api/userappid")
    Call<JsonObject> registAppid();

    @GET("api/userappid/{userAppId}")
    Call<JsonObject> getAppid(@Path("userAppId")String userAppId);

    //녹음 종료시 저장
    //json으로도 받을 수 있는 형태
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @POST("api/record")
    Call<JsonObject> addRecord(@Body RequestBody data);

    //신고하기 제출
    //json으로도 받을 수 있는 형태
//    @Headers({
//            "Accept: application/json",
//            "Content-Type: application/json"
//    })
    @PUT("api/claim/analysis/{analysisId}")
    Call<JsonObject> addClaim(@Path("analysisId") Integer analysisId, @Body RequestBody data);

    //@Query 를 이용하면 ?key=value 식으로 보내진다.
    @GET("api/record/search/findByUserAppId")
    Call<JsonObject> getRecordList(@Query("userAppId") String userAppId,@Query("sort") String sort,@Query("size") int size);

    @GET("api/recordOnly/search/findByUserAppId")
    Call<JsonObject> getOnlyRecordList(@Query("userAppId") String userAppId,@Query("sort") String sort,@Query("size") int size);

    //@Path 를 이용하면 Rest 방식으로 호출 할 수 있다.
    @GET("api/record/{recordId}")
    Call<JsonObject> getRecord(@Path("recordId") Integer recordId);

    Call<JsonObject> getClaimList(String userAppId);

    @PUT("api/userappid/{userappid}")
    Call<JsonObject> addProfile(@Path("userappid") String userappid, @Body RequestBody requestData);

    @GET("api/userappid/{userappid}")
    Call<JsonObject> getProfile(@Path("userappid") String userappid);

    @PUT("api/record/consulting/{recordId}")
    Call<JsonObject> addConsult(@Path("recordId") int recordId, @Body RequestBody requestData);

    @GET("api/statistics")
    Call<JsonObject> getStats(@Query("userappid") String userAppId,@Query("dateCd") String dateCd);

    @GET("api/recordOnly/search/findByUserAppIdAndConsultingsNotNull")
    Call<JsonObject> getConsultList(@Query("userAppId") String userAppId,@Query("sort") String sort);

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
