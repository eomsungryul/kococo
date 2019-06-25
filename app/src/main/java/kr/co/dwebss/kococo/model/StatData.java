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
package kr.co.dwebss.kococo.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

public class StatData extends RowData {
    private String mName;
    private String mTime;
    private float mAmount;
    private String mLastFourDigits;
    private String mCount;
    private int mColor;
    private DecimalFormat mFormatter;

    public StatData(String name, String time, float amount, String lastFourDigits,String count, int color) {
        this.mName = name;
        this.mAmount = amount;
        this.mTime = time;
        this.mLastFourDigits = lastFourDigits;
        this.mCount = count;
        this.mColor = color;

        this.mFormatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols symbols = this.mFormatter.getDecimalFormatSymbols();
        symbols.setCurrencySymbol("");
        this.mFormatter.setDecimalFormatSymbols(symbols);
    }

    @Override
    public String getRowName() {
        return mName;
    }

    @Override
    public String getRowSecondaryString() {
        return mLastFourDigits;
    }

    @Override
    public String getRowNameCount() {
        return mCount;
    }

    @Override
    public float getRowAmount() {
        return mAmount;
    }

    @Override
    public float getRowLimitAmount() {
        return mAmount;
    }

    @Override
    public String getRowAmountString() {
//        return mFormatter.format(mAmount);
        return mTime;
    }

    @Override
    public int getRowColor() {
        return mColor;
    }

}
