package kr.co.dwebss.kococo.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Spinner;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.adapter.DiaryListAdapter;
import kr.co.dwebss.kococo.adapter.SettingListAdapter;
import kr.co.dwebss.kococo.util.FindAppIdUtil;

public class SettingFragment extends Fragment {
    private String LOG_TAG = "SettingFragment";
    Resources res;

    public SettingFragment() {
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
        View v = inflater.inflate(R.layout.fragment_setting, container, false);
        //res/string.xml을 사용하기 위한
        res = getResources();

        // Adapter 생성
        SettingListAdapter adapter = new SettingListAdapter(getContext()) ;
        //listView 생성
        ListView listview = (ListView) v.findViewById(R.id.settingListview);
        listview.setAdapter(adapter);
        // 첫 번째 아이템 추가.
        adapter.addItem(res.getString(R.string.profile),1) ;
        adapter.addItem(res.getString(R.string.expertConsultationHistory),2) ;

//        SettingListAdapter adapter2 = new SettingListAdapter(getContext()) ;
//        ListView listview2 = (ListView) v.findViewById(R.id.settingListview2);
//        listview2.setAdapter(adapter2);
//        adapter2.addItem(res.getString(R.string.userMp3FileManage),3) ;

        return v;
    }
}
