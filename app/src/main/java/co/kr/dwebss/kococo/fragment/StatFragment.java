package co.kr.dwebss.kococo.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

import co.kr.dwebss.kococo.R;
import co.kr.dwebss.kococo.SectionAdapter;
import co.kr.dwebss.kococo.model.Account;
import co.kr.dwebss.kococo.model.RowData;
import co.kr.dwebss.kococo.model.Section;

public class StatFragment extends Fragment {

    private Section mAccountsSection;
    private RecyclerView mAccountsRV;

    public StatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View v = inflater.inflate(R.layout.fragment_stat, container, false);

        initializeData();
        mAccountsRV = v.findViewById(R.id.accounts);
        //context 를 사용하려면 getActivity를 하면됨
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAccountsRV.setLayoutManager(layoutManager);
        RecyclerView.Adapter accountsAdapter = new SectionAdapter(mAccountsSection);
        mAccountsRV.setAdapter(accountsAdapter);
        // Inflate the layout for this fragment
        return v;
    }

    private void initializeData() {
        List<RowData> accounts = new ArrayList<RowData>();
        accounts.add(new Account("총 수면시간","8시간", 2215.13f, "1234", 0xFF005F57));
        accounts.add(new Account("코골이", "1시간 20분",8676.88f, "5678", 0xFF00BD7A));
        accounts.add(new Account("무호흡","20분", 987.48f, "9012", 0xFF00F4B6));
        mAccountsSection = new Section(accounts, "수면 점수", false);
    }
}
