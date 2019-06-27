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
package kr.co.dwebss.kococo.application;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.text.TextUtils;
import android.widget.TextView;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.dwebss.kococo.R;


public class DataHolderApplication extends Application {

    private static DataHolderApplication dataHolderApplication = new DataHolderApplication();

    public static DataHolderApplication getInstance() {
        return dataHolderApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dataHolderApplication = this;
    }

    private Map<String, Object> mDataHolder = new ConcurrentHashMap<>();

    public String putDataHolder(Object data){
        //중복되지 않는 홀더 아이디를 생성해서 요청자에게 돌려준다.
        String dataHolderId = UUID.randomUUID().toString();
        mDataHolder.put(dataHolderId, data);
        return dataHolderId;
    }

    public Object popDataHolder(String key){
        Object obj = mDataHolder.get(key);
        //pop된 데이터는 홀더에서 제거
        mDataHolder.remove(key);
        return obj;
    }

}
