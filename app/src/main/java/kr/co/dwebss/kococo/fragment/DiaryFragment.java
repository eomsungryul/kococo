package kr.co.dwebss.kococo.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.MainActivity;
import kr.co.dwebss.kococo.activity.ReportActivity;
import kr.co.dwebss.kococo.adapter.DiaryListAdapter;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.Record;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class DiaryFragment extends Fragment {

    Retrofit retrofit;
    ApiService apiService;
    View v;
    DiaryListAdapter adapter;

    public DiaryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_diary, container, false);

        FindAppIdUtil fau = new FindAppIdUtil();
        String userAppId=fau.getAppid(v.getContext());
        System.out.println("DiaryFragment"+" ===============start=========response: "+userAppId);

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        apiService.getRecordList(userAppId,"recordId,desc").enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                System.out.println(" ============야야양============response: "+response);
                //저장 시에 뒤로가기
//                Toast.makeText(v.getContext(),response.body().toString(),Toast.LENGTH_LONG).show();
                Gson gson = new Gson();
                JsonObject jsonObject = response.body();
                JsonObject resultData = jsonObject.getAsJsonObject("_embedded");
                JsonArray recordList = resultData.getAsJsonArray("record");
                // Adapter 생성
                adapter = new DiaryListAdapter() ;
                //listView 생성
                ListView listview = (ListView) v.findViewById(R.id.diaryListview);
                listview.setAdapter(adapter);
                adapter.addItems(recordList) ;
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
//                System.out.println(" ========================Throwable: "+ t);
                Toast.makeText(v.getContext(),"에러입니다.",Toast.LENGTH_LONG).show();
            }
        });

        return v;
    }


    //프래그먼트 초기화 방법
    private  void refresh(){
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.detach(this).attach(this).commit();
    }


}
