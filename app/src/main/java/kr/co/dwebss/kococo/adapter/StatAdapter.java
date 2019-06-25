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
package kr.co.dwebss.kococo.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.model.RowData;
import kr.co.dwebss.kococo.model.Section;
import kr.co.dwebss.kococo.view.FractionBarView;

public class StatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Section mSection;

    public StatAdapter(Section section) {
        mSection = section;
    }

    //최초 레이아웃을 생성하는 부분
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = null;
        RecyclerView.ViewHolder vh = null;
        v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_stat_row,
                parent, false);
        vh = new StatAdapter.RowViewHolder(v);
        return vh;
    }

    //해당 레이아웃에 값을 바인드 하는 부분
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            StatAdapter.RowViewHolder rvh = (StatAdapter.RowViewHolder) holder;
            RowData rowData = mSection.getItems().get(position);
            rvh.primaryText.setText(rowData.getRowName());
            rvh.amountText.setText(rowData.getRowAmountString());

            if(!"".equals(rowData.getRowNameCount())){
                rvh.amountText.setText(rowData.getRowNameCount()+"회");
            }

            int[] colors = new int[1];
            float[] fractions = new float[1];
            colors[0] = rowData.getRowColor();
            fractions[0] = rowData.getRowAmount() / rowData.getRowLimitAmount();
            rvh.barView.setData(colors, fractions);

    }

    //이 카운트에 따라 onBindViewHolder를 수행한다.
    @Override
    public int getItemCount() {
        return mSection.getItems().size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class RowViewHolder extends RecyclerView.ViewHolder {

        public TextView primaryText;
        //        public TextView secondaryText;
//        public TextView secondaryTextObfuscation;
        public TextView amountText;
        public FractionBarView barView;
        public ImageView arrow;

        public RowViewHolder(View itemView) {
            super(itemView);
            primaryText = itemView.findViewById(R.id.account_primary_text);
//            secondaryText = itemView.findViewById(R.id.account_secondary_text);
//            secondaryTextObfuscation = itemView.findViewById(R.id
//                    .account_secondary_text_obfuscation);
            barView = itemView.findViewById(R.id.row_bar_chart);
            amountText = itemView.findViewById(R.id.amount_text);
            arrow = itemView.findViewById(R.id.arrow);
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        public TextView sectionName;
        public TextView sectionTotal1;
        public TextView sectionTotal2;
        public FractionBarView barView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
//            sectionName = itemView.findViewById(R.id.section_name);
//            sectionTotal1 = itemView.findViewById(R.id.section_total_1);
//            sectionTotal2 = itemView.findViewById(R.id.section_total_2);
//            barView = itemView.findViewById(R.id.section_bar_chart);
        }
    }

    public class SeeAllViewHolder extends RecyclerView.ViewHolder {
        public SeeAllViewHolder(View itemView) {
            super(itemView);
        }
    }
}
