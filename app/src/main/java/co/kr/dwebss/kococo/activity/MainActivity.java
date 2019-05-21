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
package co.kr.dwebss.kococo.activity;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import co.kr.dwebss.kococo.R;
import co.kr.dwebss.kococo.fragment.DiaryFragment;
import co.kr.dwebss.kococo.fragment.RecodeFragment;
import co.kr.dwebss.kococo.fragment.SettingFragment;
import co.kr.dwebss.kococo.fragment.StatFragment;
import co.kr.dwebss.kococo.main.SectionsPagerAdapter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LARGE_TAB_TEXT_SIZE = 14;
    private static final int SMALL_TAB_TEXT_SIZE = 10;
    private static final int ALERTS_PHONE_HEIGHT_DP = 68;
    private static final int ALERTS_LAPTOP_HEIGHT_DP = 800;
    private TabLayout tabs;
    private ViewPager viewPager;

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

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        setupViewPager(viewPager);

        tabs = (TabLayout)findViewById(R.id.tablayouts);
        tabs.setupWithViewPager(viewPager);
        setupTabIcons();
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged " + newConfig.screenWidthDp + "," + newConfig.screenHeightDp);

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
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {

            // return null to display only the icon
            return null;
        }
    }
}
