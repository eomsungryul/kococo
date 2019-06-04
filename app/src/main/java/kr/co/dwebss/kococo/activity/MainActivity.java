/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.dwebss.kococo.activity;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.BuildConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.fragment.DiaryFragment;
import kr.co.dwebss.kococo.fragment.RecodeFragment;
import kr.co.dwebss.kococo.fragment.SettingFragment;
import kr.co.dwebss.kococo.fragment.StatFragment;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.main.SectionsPagerAdapter;
import kr.co.dwebss.kococo.model.ApiCode;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LARGE_TAB_TEXT_SIZE = 14;
    private static final int SMALL_TAB_TEXT_SIZE = 10;
    private static final int ALERTS_PHONE_HEIGHT_DP = 68;
    private static final int ALERTS_LAPTOP_HEIGHT_DP = 800;
    private TabLayout tabs;
    private ViewPager viewPager;

    //remote config
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private static final String WELCOME_MESSAGE_KEY = "welcome_message";

    Retrofit retrofit;
    ApiService apiService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //나중에 density나 넓이 높이 등이 필요할때 사용 함
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        //기기의 스토리지에 AppIp.text가 존재하지않으면 앱ID를 생성해주고 기기에 저장하는 기능
        //두번째 로그인에는 앱아이디가 존재하므로 그냥 조회만 해줌
        //항상 사용가능하며 내부에 저장된 파일은 기본적으로 해당 앱만 접근 가능하다.
        //시스템은 앱이 제거될때 내부에 저장된 파일을 모두 제거한다.

        //앱 IP 존재 여부 확인 (Internal Storage 사용할거임)
        //Internal Storage 항상 사용가능하며 내부에 저장된 파일은 기본적으로 해당 앱만 접근 가능하다.
        //시스템은 앱이 제거될때 내부에 저장된 파일을 모두 제거한다.

        FindAppIdUtil fau = new FindAppIdUtil();
        fau.InitAppId(this);
//        InitAppId();

        //Crashlytics 강제종료 테스트
//        Crashlytics.getInstance().crash(); // Force a crash

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        //Remote Config 시작
        // Get Remote Config instance.
        // [START get_remote_config_instance]
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        // [END get_remote_config_instance]

        //개발자 모드를 사용하려면 원격 구성 설정을 작성
        // 이 모드를 사용하여 개발 중에 시간당 사용 가능한 페치 수를 늘림
        // 또한 원격 구성 설정을 사용하여 페치 간격을 설정
        // [START enable_dev_mode]
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        // 기본 Remote Config 매개 변수 값을 설정합니다. 앱은 인앱 기본 값을 사용하고
        // 이러한 기본값을 조정해야하는 경우, 사용자가 지정한 값에 대해서만 업데이트 된 값을 설정
        // [START set_default_values]
        mFirebaseRemoteConfig.setDefaults( R.xml.remote_config_defaults);
        // [end set_default_values]
        // [START fetch_config_with_callback]
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d(TAG, "Config params updated: " + updated);
//                            Toast.makeText(MainActivity.this, "Fetch and activate succeeded",Toast.LENGTH_SHORT).show();

                        } else {
//                            Toast.makeText(MainActivity.this, "Fetch failed",Toast.LENGTH_SHORT).show();
                        }
                        //여기서 함 설정 가져옴!

                        String welcomeMessage = mFirebaseRemoteConfig.getString(WELCOME_MESSAGE_KEY);
//                        Toast.makeText(MainActivity.this, welcomeMessage,Toast.LENGTH_SHORT).show();
                    }
                });
        // [END fetch_config_with_callback]
        //Remote Config 끝

        //firebase storage 시작
        // [START storage_field_initialization]
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // [END storage_field_initialization]
        includesForCreateReference();
        //firebase storage 끝



        viewPager = (ViewPager) findViewById(R.id.view_pager);
        setupViewPager(viewPager);

        tabs = (TabLayout)findViewById(R.id.tablayouts);
        tabs.setupWithViewPager(viewPager);
        setupTabIcons();
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                onSlideChanged(position); // change color of the dots
            }
            @Override
            public void onPageSelected(int position) {}
            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        //http 통신
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        System.out.println(" ===========http 통신=============statr: ");
        apiService.getApiCode().enqueue(new Callback<ApiCode>() {
            @Override
            public void onResponse(Call<ApiCode> call, Response<ApiCode> response) {
//                Toast.makeText(MainActivity.this, "sucess"+response,Toast.LENGTH_SHORT).show();

                System.out.println(" ==========http 통신==============response: "+response.body().toString());

                ApiCode result = response.body();
                result.getEmbedded().getCode().get(0).getCode();
                System.out.println(" ===========http 통신=============result.getEmbedded().getCode().get(0).getCode();: "+result.getEmbedded().getCode().get(0).getCode());
                System.out.println(" ===========http 통신=============result.getEmbedded().getCode().get(0).getCode();: "+result.getEmbedded().getCode().get(0).getCodeCateogry());
                System.out.println(" ============http 통신============result.getEmbedded().getCode().get(0).getCode();: "+result.getEmbedded().getCode().get(1).getCode());
                System.out.println(" =============http 통신===========result.getEmbedded().getCode().get(0).getCode();: "+result.getEmbedded().getCode().get(1).getCodeCateogry());
            }

            @Override
            public void onFailure(Call<ApiCode> call, Throwable t) {

            }
        });

//        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
//        apiService = retrofit.create(ApiService.class);

        apiService.getApiCode2().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                System.out.println(" ===========getApiCode2=============response22: "+response);
                System.out.println(" ===========getApiCode2=============response22: "+response.body().toString());

                JsonObject result = response.body();
                System.out.println(" ============getApiCode2============result: "+result);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
////

    }
    @Override
    public void onResume() {
        super.onResume();
        System.out.println(" ============onResume============: ");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged " + newConfig.screenWidthDp + "," + newConfig.screenHeightDp);
        System.out.println(" ============onConfigurationChanged============: ");

    }

    private void setupTabIcons() {
        int[] tabIcons = {
                R.drawable.outline_mic_none_white_48dp,
                R.drawable.outline_event_note_white_48dp,
                R.drawable.outline_equalizer_white_48dp,
                R.drawable.outline_settings_white_48dp
        };

        tabs.getTabAt(0).setIcon(tabIcons[0]);
        tabs.getTabAt(1).setIcon(tabIcons[1]);
        tabs.getTabAt(2).setIcon(tabIcons[2]);
        tabs.getTabAt(3).setIcon(tabIcons[3]);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new RecodeFragment(), "RecodeFragment");
        adapter.addFrag(new DiaryFragment(), "Diary");
        adapter.addFrag(new StatFragment(), "Stat");
        adapter.addFrag(new SettingFragment(), "Setting");
        adapter.notifyDataSetChanged();
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentStatePagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public int getItemPosition(Object object) {
//            if (object instanceof DiaryFragment) {
//
//                System.out.println("=================if=======getItemPosition : "+object);
//
//                return super.getItemPosition(object);
//            } else {
//                System.out.println("=================else=======getItemPosition : "+object);
//                return POSITION_NONE;
//            }
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            System.out.println("========================getItem : "+position);
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            System.out.println("========================addFrag : "+title);
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {

            // return null to display only the icon
            return null;
        }

    }

    public void includesForCreateReference() {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // ## Create a Reference

        // [START create_storage_reference]
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();
        // [END create_storage_reference]

        // [START create_child_reference]
        // Create a child reference
        // imagesRef now points to "images"
        StorageReference imagesRef = storageRef.child("images");

        // Child references can also take paths
        // spaceRef now points to "images/space.jpg
        // imagesRef still points to "images"
        StorageReference spaceRef = storageRef.child("images/space.jpg");
        // [END create_child_reference]

        // ## Navigate with References

        // [START navigate_references]
        // getParent allows us to move our reference to a parent node
        // imagesRef now points to 'images'
        imagesRef = spaceRef.getParent();

        // getRoot allows us to move all the way back to the top of our bucket
        // rootRef now points to the root
        StorageReference rootRef = spaceRef.getRoot();
        // [END navigate_references]

        // [START chain_navigation]
        // References can be chained together multiple times
        // earthRef points to 'images/earth.jpg'
        StorageReference earthRef = spaceRef.getParent().child("earth.jpg");

        // nullRef is null, since the parent of root is null
        StorageReference nullRef = spaceRef.getRoot().getParent();
        // [END chain_navigation]

        // ## Reference Properties

        // [START reference_properties]
        // Reference's path is: "images/space.jpg"
        // This is analogous to a file path on disk
        spaceRef.getPath();

        // Reference's name is the last segment of the full path: "space.jpg"
        // This is analogous to the file name
        spaceRef.getName();

        // Reference's bucket is the name of the storage bucket that the files are stored in
        spaceRef.getBucket();
        // [END reference_properties]

        // ## Full Example

        // [START reference_full_example]
        // Points to the root reference
        storageRef = storage.getReference();

        // Points to "images"
        imagesRef = storageRef.child("images");

        // Points to "images/space.jpg"
        // Note that you can use variables to create child values
        String fileName = "space.jpg";
        spaceRef = imagesRef.child(fileName);

        // File path is "images/space.jpg"
        String path = spaceRef.getPath();

        // File name is "space.jpg"
        String name = spaceRef.getName();

        // Points to "images"
        imagesRef = spaceRef.getParent();
        // [END reference_full_example]
    }

}
