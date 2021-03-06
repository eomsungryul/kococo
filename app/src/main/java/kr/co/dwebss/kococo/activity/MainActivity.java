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

import android.app.Activity;
import android.content.res.Configuration;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.BuildConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.application.VersionProgressApplication;
import kr.co.dwebss.kococo.billing.BillingManager;
import kr.co.dwebss.kococo.fragment.DiaryFragment;
import kr.co.dwebss.kococo.fragment.RecordFragment;
import kr.co.dwebss.kococo.fragment.SettingFragment;
import kr.co.dwebss.kococo.fragment.StatFragment;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.main.SectionsPagerAdapter;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import kr.co.dwebss.kococo.util.TabLayoutUtils;
import retrofit2.Retrofit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String TAG_YRSEO = "yrseo";

    private static final int LARGE_TAB_TEXT_SIZE = 14;
    private static final int SMALL_TAB_TEXT_SIZE = 10;
    private static final int ALERTS_PHONE_HEIGHT_DP = 68;
    private static final int ALERTS_LAPTOP_HEIGHT_DP = 800;
    private TabLayout tabs;
    private ViewPager viewPager;
    Activity thisMainActivity;

    //remote config
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private static final String WELCOME_MESSAGE_KEY = "welcome_message";

    Retrofit retrofit;
    ApiService apiService;

    VersionProgressApplication vpa = new VersionProgressApplication();


    //전면광고
    //private InterstitialAd mInterstitialAd;
    private RewardedAd rewardedAd;
//    private AdView adView;
//    private AdRequest adRequest;

    //결제
    private BillingClient billingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisMainActivity=this;

//        billingClient = BillingClient.newBuilder(getApplication()).setListener(new PurchasesUpdatedListener() {
//            @Override
//            public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
//                System.out.println("========bill=====onPurchasesUpdated===========");
//            }
//        }).build();
//        billingClient.startConnection(new BillingClientStateListener() {
//            @Override
//            public void onBillingSetupFinished(BillingResult billingResult) {
//                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                    // The BillingClient is ready. You can query purchases here.
//                    System.out.println("========bill=====onBillingSetupFinished===========");
//                }
//            }
//            @Override
//            public void onBillingServiceDisconnected() {
//                // Try to restart the connection on the next request to
//                // Google Play by calling the startConnection() method.
//                System.out.println("========bill=====onBillingServiceDisconnected===========");
//            }
//        });

        MobileAds.initialize(this, StaticVariables.REAL_AD_APP_KEY);
//        //배너광고
//        adView = (AdView)findViewById(R.id.publisherAdView);
//
//        adRequest = new AdRequest.Builder().build();
        // xml에 세팅하고 또 세팅하면  The ad size can only be set once on AdView. 에러 발생
//                adView.setAdSize(AdSize.FULL_BANNER);
        //adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
//        adView.loadAd(adRequest);


//        mPublisherAdView = findViewById(R.id.publisherAdView);
//        PublisherAdRequest adRequest = new PublisherAdRequest.Builder().build();
//        mPublisherAdView.loadAd(adRequest);

        //전면광고
        //mInterstitialAd = new InterstitialAd(this);
        rewardedAd = new RewardedAd(this, StaticVariables.REAL_REWARD_AD_KEY); //실제
        //rewardedAd = new RewardedAd(this, StaticVariables.TEST_REWARD_AD_KEY); //테스트
        //테스트값
        //mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        //광고불러오기
        //mInterstitialAd.loadAd(new AdRequest.Builder().build());
        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                Log.e("ad","onRewardedAdLoaded");
            }

            @Override
            public void onRewardedAdFailedToLoad(int errorCode) {
                Log.e("ad","onRewardedAdFailedToLoad: "+errorCode);
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);

        //나중에 density나 넓이 높이 등이 필요할때 사용 함
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

//        vpa = new VersionProgressApplication();
//        vpa.progressON(this,"");

        //기기의 스토리지에 AppIp.text가 존재하지않으면 앱ID를 생성해주고 기기에 저장하는 기능
        //두번째 로그인에는 앱아이디가 존재하므로 그냥 조회만 해줌
        //항상 사용가능하며 내부에 저장된 파일은 기본적으로 해당 앱만 접근 가능하다.
        //시스템은 앱이 제거될때 내부에 저장된 파일을 모두 제거한다.

        //앱 IP 존재 여부 확인 (Internal Storage 사용할거임)
        //Internal Storage 항상 사용가능하며 내부에 저장된 파일은 기본적으로 해당 앱만 접근 가능하다.
        //시스템은 앱이 제거될때 내부에 저장된 파일을 모두 제거한다.

        FindAppIdUtil fau = new FindAppIdUtil();
        fau.InitAppId(this);

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
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
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

                        System.out.println("=========welcomeMessage============" + welcomeMessage);
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

        tabs = (TabLayout) findViewById(R.id.tablayouts);
        tabs.setupWithViewPager(viewPager);
        setupTabIcons();

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Log.e(TAG_YRSEO,"postion: "+position);
                if(position!=0){
//                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress) ;
//                    progressBar.setVisibility(View.INVISIBLE);
//                    TextView progressBarTxt = (TextView) findViewById(R.id.progressTxt) ;
//                    progressBarTxt.setVisibility(View.INVISIBLE);
                }else{
                    vpa.progressON(thisMainActivity);
//                    chkVersion();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        vpa.progressON(this);
//                    chkVersion();
    }
    //onResume()은 Activity가 사용자와 상호작용을 하기 직전에 호출됩니다. 스택의 최상위에 위치하여 Activity를 활성화
    @Override
    public void onResume() {
        super.onResume();
    }

    public void tabDisable(){
        //뷰 페이저 쪽의 페이지 리스너를 클리어한다.
        viewPager.clearOnPageChangeListeners();
        //탭쪽의 페이지 리스너를 클리어한다.
        tabs.clearOnTabSelectedListeners();
        //탭 터치시 탭바 이동을 막는 부분
        TabLayoutUtils.enableTabs(tabs,false);
        //스와이프 막는 부분
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }
    public void tabEnable(){
        TabLayoutUtils.enableTabs(tabs,true);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //다시 리스너를 만들때는 뷰페이저를 이동하라고 해줘야 뷰페이저가 움직임..
                viewPager.setCurrentItem(tab.getPosition());
                if(tab.getPosition()!=0){
//                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress) ;
//                    progressBar.setVisibility(View.INVISIBLE);
//                    TextView progressBarTxt = (TextView) findViewById(R.id.progressTxt) ;
//                    progressBarTxt.setVisibility(View.INVISIBLE);
                }else{
                    vpa.progressON(thisMainActivity);
                    //chkVersion();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                System.out.println("============onTabUnselected");
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                System.out.println("============onTabReselected");
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageSelected(int position) {
                //그냥 스와이프 할 시에 탭아이콘이 움직이지않음.. 그래서 추가해야함
                tabs.getTabAt(position).select();
            }
            @Override
            public void onPageScrollStateChanged(int state) {}
        });
        //스와이프 막는 부분
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

    }

    // 화면전환이 일어난 경우에만 호출된다고 생각하지만 Locale 이나 각종 설정값이 바꼇을 경우도 호출
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
        adapter.addFrag(new RecordFragment(new RecordFragment.TabEventUtil() {
            @Override
            public void tabEvent(boolean flag) {
                if(flag){
                    tabDisable();
                }else{
                    tabEnable();
                }
            }
        },rewardedAd), "RecodeFragment");
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
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            System.out.println("========================getItem : "+position);
            Log.e(TAG_YRSEO,"postion: "+position);
            if(position!=0){
//                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress) ;
//                progressBar.setVisibility(View.INVISIBLE);
//                TextView progressBarTxt = (TextView) findViewById(R.id.progressTxt) ;
//                progressBarTxt.setVisibility(View.INVISIBLE);
            }else{
//                vpa.progressON(thisMainActivity);
                //chkVersion();
            }
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
