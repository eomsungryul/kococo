package co.kr.dwebss.kococo.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import co.kr.dwebss.kococo.R;
import co.kr.dwebss.kococo.ResultActivity;


public class RecodeFragment extends Fragment {
    Boolean recodeFlag = false;

    public RecodeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_recode, container, false);
        Button recodeBtn = (Button) v.findViewById(R.id.recodeBtn) ;
        recodeFlag = false;
        recodeBtn.setText("녹음 시작");
        recodeBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( recodeFlag == false){
                    recodeBtn.setText("녹음 종료");
                    recodeFlag = true;
                }else{
                    //창 띄우기
                    startActivity(new Intent(getActivity(), ResultActivity.class));
                    recodeFlag = false;
                    recodeBtn.setText("녹음 시작");
                }
            }
        });
        // Inflate the layout for this fragment
        return v;
    }

}
