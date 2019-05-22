package co.kr.dwebss.kococo.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import co.kr.dwebss.kococo.R;
import co.kr.dwebss.kococo.adapter.DiaryListAdapter;
import co.kr.dwebss.kococo.adapter.RecordListAdapter;


public class DiaryFragment extends Fragment {

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
        View v = inflater.inflate(R.layout.fragment_diary, container, false);

        // Adapter 생성
        DiaryListAdapter adapter = new DiaryListAdapter() ;
        //listView 생성
        ListView listview = (ListView) v.findViewById(R.id.diaryListview);
        listview.setAdapter(adapter);
        // 첫 번째 아이템 추가.
        adapter.addItem("몇월 몇일 맑음") ;
        adapter.addItem("녹음파일2") ;
        adapter.addItem("녹음파일3") ;
        adapter.addItem("녹음파일4") ;
        adapter.addItem("녹음파일5") ;
        // 두 번째 아이템 추가.
//        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.ic_account_circle_black_36dp),
//                "Circle", "Account Circle Black 36dp") ;

        return v;
    }

}
