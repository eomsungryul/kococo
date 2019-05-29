package kr.co.dwebss.kococo.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.adapter.DiaryListAdapter;

public class SettingFragment extends Fragment {


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



        // Adapter 생성
        DiaryListAdapter adapter = new DiaryListAdapter() ;
        //listView 생성
        ListView listview = (ListView) v.findViewById(R.id.settingListview);
        listview.setAdapter(adapter);
        // 첫 번째 아이템 추가.
//        adapter.addItem("프로필") ;
//
//        DiaryListAdapter adapter2 = new DiaryListAdapter() ;
//        ListView listview2 = (ListView) v.findViewById(R.id.settingListview2);
//        listview2.setAdapter(adapter2);
//        adapter2.addItem("전문가 상담 내역") ;
//        adapter2.addItem("의견 및 제안") ;
        // 두 번째 아이템 추가.
//        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.ic_account_circle_black_36dp),
//                "Circle", "Account Circle Black 36dp") ;

        return v;
    }
}
