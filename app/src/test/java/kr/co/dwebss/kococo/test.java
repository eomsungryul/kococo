package kr.co.dwebss.kococo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class test {


    static int standardMinite =0;
    static float  standardDb =0;

    public static void main(String[] arge){

        String test = "[{\\\"TIME\\\":\\\"56\\\",\\\"DB\\\":\\\"44.20\\\"},{\\\"TIME\\\":\\\"57\\\",\\\"DB\\\":\\\"51.38\\\"},{\\\"TIME\\\":\\\"58\\\",\\\"DB\\\":\\\"67.85\\\"},{\\\"TIME\\\":\\\"59\\\",\\\"DB\\\":\\\"57.69\\\"},{\\\"TIME\\\":\\\"60\\\",\\\"DB\\\":\\\"62.49\\\"},{\\\"TIME\\\":\\\"61\\\",\\\"DB\\\":\\\"46.97\\\"},{\\\"TIME\\\":\\\"62\\\",\\\"DB\\\":\\\"47.50\\\"},{\\\"TIME\\\":\\\"63\\\",\\\"DB\\\":\\\"45.55\\\"},{\\\"TIME\\\":\\\"64\\\",\\\"DB\\\":\\\"66.78\\\"},{\\\"TIME\\\":\\\"65\\\",\\\"DB\\\":\\\"57.25\\\"},{\\\"TIME\\\":\\\"66\\\",\\\"DB\\\":\\\"61.95\\\"},{\\\"TIME\\\":\\\"67\\\",\\\"DB\\\":\\\"58.09\\\"},{\\\"TIME\\\":\\\"68\\\",\\\"DB\\\":\\\"61.24\\\"},{\\\"TIME\\\":\\\"69\\\",\\\"DB\\\":\\\"58.81\\\"},{\\\"TIME\\\":\\\"70\\\",\\\"DB\\\":\\\"55.81\\\"}]\"}],\"claimYn\":\"N\",\"recordingData\":\"[{\\\"TIME\\\":\\\"1\\\",\\\"DB\\\":\\\"26.32\\\"},{\\\"TIME\\\":\\\"2\\\",\\\"DB\\\":\\\"26.32\\\"},{\\\"TIME\\\":\\\"3\\\",\\\"DB\\\":\\\"59.21\\\"},{\\\"TIME\\\":\\\"4\\\",\\\"DB\\\":\\\"58.29\\\"},{\\\"TIME\\\":\\\"5\\\",\\\"DB\\\":\\\"31.96\\\"},{\\\"TIME\\\":\\\"6\\\",\\\"DB\\\":\\\"25.64\\\"},{\\\"TIME\\\":\\\"7\\\",\\\"DB\\\":\\\"26.48\\\"},{\\\"TIME\\\":\\\"8\\\",\\\"DB\\\":\\\"26.75\\\"},{\\\"TIME\\\":\\\"9\\\",\\\"DB\\\":\\\"26.93\\\"},{\\\"TIME\\\":\\\"10\\\",\\\"DB\\\":\\\"27.49\\\"},{\\\"TIME\\\":\\\"11\\\",\\\"DB\\\":\\\"27.88\\\"},{\\\"TIME\\\":\\\"12\\\",\\\"DB\\\":\\\"54.28\\\"},{\\\"TIME\\\":\\\"13\\\",\\\"DB\\\":\\\"62.81\\\"},{\\\"TIME\\\":\\\"14\\\",\\\"DB\\\":\\\"61.70\\\"},{\\\"TIME\\\":\\\"15\\\",\\\"DB\\\":\\\"33.09\\\"},{\\\"TIME\\\":\\\"16\\\",\\\"DB\\\":\\\"38.62\\\"},{\\\"TIME\\\":\\\"17\\\",\\\"DB\\\":\\\"56.55\\\"},{\\\"TIME\\\":\\\"18\\\",\\\"DB\\\":\\\"62.37\\\"},{\\\"TIME\\\":\\\"19\\\",\\\"DB\\\":\\\"40.34\\\"},{\\\"TIME\\\":\\\"20\\\",\\\"DB\\\":\\\"34.00\\\"},{\\\"TIME\\\":\\\"21\\\",\\\"DB\\\":\\\"55.10\\\"},{\\\"TIME\\\":\\\"22\\\",\\\"DB\\\":\\\"61.89\\\"},{\\\"TIME\\\":\\\"23\\\",\\\"DB\\\":\\\"57.63\\\"},{\\\"TIME\\\":\\\"24\\\",\\\"DB\\\":\\\"35.05\\\"},{\\\"TIME\\\":\\\"25\\\",\\\"DB\\\":\\\"38.20\\\"},{\\\"TIME\\\":\\\"26\\\",\\\"DB\\\":\\\"49.57\\\"},{\\\"TIME\\\":\\\"27\\\",\\\"DB\\\":\\\"49.61\\\"},{\\\"TIME\\\":\\\"28\\\",\\\"DB\\\":\\\"36.60\\\"},{\\\"TIME\\\":\\\"29\\\",\\\"DB\\\":\\\"42.02\\\"},{\\\"TIME\\\":\\\"30\\\",\\\"DB\\\":\\\"35.40\\\"},{\\\"TIME\\\":\\\"31\\\",\\\"DB\\\":\\\"65.43\\\"},{\\\"TIME\\\":\\\"32\\\",\\\"DB\\\":\\\"57.37\\\"},{\\\"TIME\\\":\\\"33\\\",\\\"DB\\\":\\\"37.85\\\"},{\\\"TIME\\\":\\\"34\\\",\\\"DB\\\":\\\"39.50\\\"},{\\\"TIME\\\":\\\"35\\\",\\\"DB\\\":\\\"56.34\\\"},{\\\"TIME\\\":\\\"36\\\",\\\"DB\\\":\\\"64.33\\\"},{\\\"TIME\\\":\\\"37\\\",\\\"DB\\\":\\\"58.14\\\"},{\\\"TIME\\\":\\\"38\\\",\\\"DB\\\":\\\"39.38\\\"},{\\\"TIME\\\":\\\"39\\\",\\\"DB\\\":\\\"46.30\\\"},{\\\"TIME\\\":\\\"40\\\",\\\"DB\\\":\\\"67.08\\\"},{\\\"TIME\\\":\\\"41\\\",\\\"DB\\\":\\\"62.97\\\"},{\\\"TIME\\\":\\\"42\\\",\\\"DB\\\":\\\"43.33\\\"},{\\\"TIME\\\":\\\"43\\\",\\\"DB\\\":\\\"41.28\\\"},{\\\"TIME\\\":\\\"44\\\",\\\"DB\\\":\\\"64.45\\\"},{\\\"TIME\\\":\\\"45\\\",\\\"DB\\\":\\\"59.63\\\"},{\\\"TIME\\\":\\\"46\\\",\\\"DB\\\":\\\"39.32\\\"},{\\\"TIME\\\":\\\"47\\\",\\\"DB\\\":\\\"41.16\\\"},{\\\"TIME\\\":\\\"48\\\",\\\"DB\\\":\\\"67.91\\\"},{\\\"TIME\\\":\\\"49\\\",\\\"DB\\\":\\\"64.98\\\"},{\\\"TIME\\\":\\\"50\\\",\\\"DB\\\":\\\"59.45\\\"},{\\\"TIME\\\":\\\"51\\\",\\\"DB\\\":\\\"39.94\\\"},{\\\"TIME\\\":\\\"52\\\",\\\"DB\\\":\\\"40.74\\\"},{\\\"TIME\\\":\\\"53\\\",\\\"DB\\\":\\\"65.09\\\"},{\\\"TIME\\\":\\\"54\\\",\\\"DB\\\":\\\"64.96\\\"},{\\\"TIME\\\":\\\"55\\\",\\\"DB\\\":\\\"42.82\\\"},{\\\"TIME\\\":\\\"56\\\",\\\"DB\\\":\\\"44.20\\\"},{\\\"TIME\\\":\\\"57\\\",\\\"DB\\\":\\\"51.38\\\"},{\\\"TIME\\\":\\\"58\\\",\\\"DB\\\":\\\"67.85\\\"},{\\\"TIME\\\":\\\"59\\\",\\\"DB\\\":\\\"57.69\\\"},{\\\"TIME\\\":\\\"60\\\",\\\"DB\\\":\\\"62.49\\\"},{\\\"TIME\\\":\\\"61\\\",\\\"DB\\\":\\\"46.97\\\"},{\\\"TIME\\\":\\\"62\\\",\\\"DB\\\":\\\"47.50\\\"},{\\\"TIME\\\":\\\"63\\\",\\\"DB\\\":\\\"45.55\\\"},{\\\"TIME\\\":\\\"64\\\",\\\"DB\\\":\\\"66.78\\\"},{\\\"TIME\\\":\\\"65\\\",\\\"DB\\\":\\\"57.25\\\"},{\\\"TIME\\\":\\\"66\\\",\\\"DB\\\":\\\"61.95\\\"},{\\\"TIME\\\":\\\"67\\\",\\\"DB\\\":\\\"58.09\\\"},{\\\"TIME\\\":\\\"68\\\",\\\"DB\\\":\\\"61.24\\\"},{\\\"TIME\\\":\\\"69\\\",\\\"DB\\\":\\\"58.81\\\"},{\\\"TIME\\\":\\\"70\\\",\\\"DB\\\":\\\"55.81\\\"},{\\\"TIME\\\":\\\"71\\\",\\\"DB\\\":\\\"58.60\\\"},{\\\"TIME\\\":\\\"72\\\",\\\"DB\\\":\\\"44.31\\\"},{\\\"TIME\\\":\\\"73\\\",\\\"DB\\\":\\\"49.59\\\"},{\\\"TIME\\\":\\\"74\\\",\\\"DB\\\":\\\"50.64\\\"},{\\\"TIME\\\":\\\"75\\\",\\\"DB\\\":\\\"50.72\\\"},{\\\"TIME\\\":\\\"76\\\",\\\"DB\\\":\\\"48.64\\\"},{\\\"TIME\\\":\\\"77\\\",\\\"DB\\\":\\\"45.98\\\"},{\\\"TIME\\\":\\\"78\\\",\\\"DB\\\":\\\"48.94\\\"},{\\\"TIME\\\":\\\"79\\\",\\\"DB\\\":\\\"47.56\\\"},{\\\"TIME\\\":\\\"80\\\",\\\"DB\\\":\\\"48.34\\\"},{\\\"TIME\\\":\\\"81\\\",\\\"DB\\\":\\\"48.59\\\"},{\\\"TIME\\\":\\\"82\\\",\\\"DB\\\":\\\"51.70\\\"},{\\\"TIME\\\":\\\"83\\\",\\\"DB\\\":\\\"52.73\\\"},{\\\"TIME\\\":\\\"84\\\",\\\"DB\\\":\\\"52.02\\\"},{\\\"TIME\\\":\\\"85\\\",\\\"DB\\\":\\\"53.50\\\"},{\\\"TIME\\\":\\\"86\\\",\\\"DB\\\":\\\"51.60\\\"},{\\\"TIME\\\":\\\"87\\\",\\\"DB\\\":\\\"54.90\\\"},{\\\"TIME\\\":\\\"88\\\",\\\"DB\\\":\\\"53.79\\\"},{\\\"TIME\\\":\\\"89\\\",\\\"DB\\\":\\\"53.34\\\"},{\\\"TIME\\\":\\\"90\\\",\\\"DB\\\":\\\"54.53\\\"},{\\\"TIME\\\":\\\"91\\\",\\\"DB\\\":\\\"58.41\\\"},{\\\"TIME\\\":\\\"92\\\",\\\"DB\\\":\\\"59.19\\\"},{\\\"TIME\\\":\\\"93\\\",\\\"DB\\\":\\\"49.08\\\"},{\\\"TIME\\\":\\\"94\\\",\\\"DB\\\":\\\"59.29\\\"},{\\\"TIME\\\":\\\"95\\\",\\\"DB\\\":\\\"55.92\\\"},{\\\"TIME\\\":\\\"96\\\",\\\"DB\\\":\\\"58.98\\\"},{\\\"TIME\\\":\\\"97\\\",\\\"DB\\\":\\\"57.90\\\"},{\\\"TIME\\\":\\\"98\\\",\\\"DB\\\":\\\"57.89\\\"},{\\\"TIME\\\":\\\"99\\\",\\\"DB\\\":\\\"45.47\\\"},{\\\"TIME\\\":\\\"100\\\",\\\"DB\\\":\\\"45.96\\\"},{\\\"TIME\\\":\\\"101\\\",\\\"DB\\\":\\\"47.18\\\"},{\\\"TIME\\\":\\\"102\\\",\\\"DB\\\":\\\"48.72\\\"},{\\\"TIME\\\":\\\"103\\\",\\\"DB\\\":\\\"39.94\\\"},{\\\"TIME\\\":\\\"104\\\",\\\"DB\\\":\\\"29.89\\\"},{\\\"TIME\\\":\\\"105\\\",\\\"DB\\\":\\\"29.52\\\"},{\\\"TIME\\\":\\\"106\\\",\\\"DB\\\":\\\"32.70\\\"},{\\\"TIME\\\":\\\"107\\\",\\\"DB\\\":\\\"44.67\\\"},{\\\"TIME\\\":\\\"108\\\",\\\"DB\\\":\\\"61.49\\\"},{\\\"TIME\\\":\\\"109\\\",\\\"DB\\\":\\\"57.35\\\"},{\\\"TIME\\\":\\\"110\\\",\\\"DB\\\":\\\"56.98\\\"},{\\\"TIME\\\":\\\"111\\\",\\\"DB\\\":\\\"58.03\\\"},{\\\"TIME\\\":\\\"112\\\",\\\"DB\\\":\\\"50.72\\\"},{\\\"TIME\\\":\\\"113\\\",\\\"DB\\\":\\\"52.92\\\"},{\\\"TIME\\\":\\\"114\\\",\\\"DB\\\":\\\"43.83\\\"},{\\\"TIME\\\":\\\"115\\\",\\\"DB\\\":\\\"30.52\\\"},{\\\"TIME\\\":\\\"116\\\",\\\"DB\\\":\\\"31.32\\\"},{\\\"TIME\\\":\\\"117\\\",\\\"DB\\\":\\\"30.06\\\"},{\\\"TIME\\\":\\\"118\\\",\\\"DB\\\":\\\"30.91\\\"},{\\\"TIME\\\":\\\"119\\\",\\\"DB\\\":\\\"30.04\\\"},{\\\"TIME\\\":\\\"120\\\",\\\"DB\\\":\\\"48.79\\\"},{\\\"TIME\\\":\\\"121\\\",\\\"DB\\\":\\\"58.87\\\"},{\\\"TIME\\\":\\\"122\\\",\\\"DB\\\":\\\"53.45\\\"},{\\\"TIME\\\":\\\"123\\\",\\\"DB\\\":\\\"46.73\\\"},{\\\"TIME\\\":\\\"124\\\",\\\"DB\\\":\\\"43.34\\\"},{\\\"TIME\\\":\\\"125\\\",\\\"DB\\\":\\\"36.38\\\"},{\\\"TIME\\\":\\\"126\\\",\\\"DB\\\":\\\"38.14\\\"},{\\\"TIME\\\":\\\"127\\\",\\\"DB\\\":\\\"28.83\\\"},{\\\"TIME\\\":\\\"128\\\",\\\"DB\\\":\\\"33.52\\\"},{\\\"TIME\\\":\\\"129\\\",\\\"DB\\\":\\\"54.44\\\"},{\\\"TIME\\\":\\\"130\\\",\\\"DB\\\":\\\"53.29\\\"},{\\\"TIME\\\":\\\"131\\\",\\\"DB\\\":\\\"63.83\\\"},{\\\"TIME\\\":\\\"132\\\",\\\"DB\\\":\\\"52.21\\\"},{\\\"TIME\\\":\\\"133\\\",\\\"DB\\\":\\\"51.58\\\"},{\\\"TIME\\\":\\\"134\\\",\\\"DB\\\":\\\"53.57\\\"},{\\\"TIME\\\":\\\"135\\\",\\\"DB\\\":\\\"55.97\\\"},{\\\"TIME\\\":\\\"136\\\",\\\"DB\\\":\\\"59.79\\\"},{\\\"TIME\\\":\\\"137\\\",\\\"DB\\\":\\\"57.60\\\"},{\\\"TIME\\\":\\\"138\\\",\\\"DB\\\":\\\"52.68\\\"},{\\\"TIME\\\":\\\"139\\\",\\\"DB\\\":\\\"46.58\\\"},{\\\"TIME\\\":\\\"140\\\",\\\"DB\\\":\\\"47.35\\\"},{\\\"TIME\\\":\\\"141\\\",\\\"DB\\\":\\\"28.58\\\"},{\\\"TIME\\\":\\\"142\\\",\\\"DB\\\":\\\"27.48\\\"},{\\\"TIME\\\":\\\"143\\\",\\\"DB\\\":\\\"25.43\\\"},{\\\"TIME\\\":\\\"144\\\",\\\"DB\\\":\\\"27.00\\\"},{\\\"TIME\\\":\\\"145\\\",\\\"DB\\\":\\\"29.10\\\"},{\\\"TIME\\\":\\\"146\\\",\\\"DB\\\":\\\"33.58\\\"},{\\\"TIME\\\":\\\"147\\\",\\\"DB\\\":\\\"28.57\\\"},{\\\"TIME\\\":\\\"148\\\",\\\"DB\\\":\\\"28.79\\\"},{\\\"TIME\\\":\\\"149\\\",\\\"DB\\\":\\\"28.23\\\"},{\\\"TIME\\\":\\\"150\\\",\\\"DB\\\":\\\"28.08\\\"},{\\\"TIME\\\":\\\"151\\\",\\\"DB\\\":\\\"26.76\\\"},{\\\"TIME\\\":\\\"152\\\",\\\"DB\\\":\\\"25.38\\\"},{\\\"TIME\\\":\\\"153\\\",\\\"DB\\\":\\\"47.08\\\"},{\\\"TIME\\\":\\\"154\\\",\\\"DB\\\":\\\"27.08\\\"},{\\\"TIME\\\":\\\"155\\\",\\\"DB\\\":\\\"26.95\\\"},{\\\"TIME\\\":\\\"156\\\",\\\"DB\\\":\\\"25.04\\\"},{\\\"TIME\\\":\\\"157\\\",\\\"DB\\\":\\\"28.34\\\"},{\\\"TIME\\\":\\\"158\\\",\\\"DB\\\":\\\"26.91\\\"},{\\\"TIME\\\":\\\"159\\\",\\\"DB\\\":\\\"27.66\\\"},{\\\"TIME\\\":\\\"160\\\",\\\"DB\\\":\\\"27.79\\\"},{\\\"TIME\\\":\\\"161\\\",\\\"DB\\\":\\\"28.13\\\"},{\\\"TIME\\\":\\\"162\\\",\\\"DB\\\":\\\"52.68\\\"},{\\\"TIME\\\":\\\"163\\\",\\\"DB\\\":\\\"33.44\\\"},{\\\"TIME\\\":\\\"164\\\",\\\"DB\\\":\\\"28.33\\\"},{\\\"TIME\\\":\\\"165\\\",\\\"DB\\\":\\\"32.30\\\"},{\\\"TIME\\\":\\\"166\\\",\\\"DB\\\":\\\"27.04\\\"},{\\\"TIME\\\":\\\"167\\\",\\\"DB\\\":\\\"28.17\\\"},{\\\"TIME\\\":\\\"168\\\",\\\"DB\\\":\\\"28.15\\\"},{\\\"TIME\\\":\\\"169\\\",\\\"DB\\\":\\\"29.80\\\"},{\\\"TIME\\\":\\\"170\\\",\\\"DB\\\":\\\"29.84\\\"},{\\\"TIME\\\":\\\"171\\\",\\\"DB\\\":\\\"26.75\\\"},{\\\"TIME\\\":\\\"172\\\",\\\"DB\\\":\\\"27.36\\\"},{\\\"TIME\\\":\\\"173\\\",\\\"DB\\\":\\\"26.12\\\"},{\\\"TIME\\\":\\\"174\\\",\\\"DB\\\":\\\"40.11\\\"},{\\\"TIME\\\":\\\"175\\\",\\\"DB\\\":\\\"27.44\\\"},{\\\"TIME\\\":\\\"176\\\",\\\"DB\\\":\\\"27.38\\\"},{\\\"TIME\\\":\\\"177\\\",\\\"DB\\\":\\\"26.97\\\"},{\\\"TIME\\\":\\\"178\\\",\\\"DB\\\":\\\"32.53\\\"},{\\\"TIME\\\":\\\"179\\\",\\\"DB\\\":\\\"55.52\\\"},{\\\"TIME\\\":\\\"180\\\",\\\"DB\\\":\\\"50.40\\\"},{\\\"TIME\\\":\\\"181\\\",\\\"DB\\\":\\\"49.80\\\"},{\\\"TIME\\\":\\\"182\\\",\\\"DB\\\":\\\"44.88\\\"},{\\\"TIME\\\":\\\"183\\\",\\\"DB\\\":\\\"31.09\\\"},{\\\"TIME\\\":\\\"184\\\",\\\"DB\\\":\\\"35.85\\\"},{\\\"TIME\\\":\\\"185\\\",\\\"DB\\\":\\\"41.21\\\"},{\\\"TIME\\\":\\\"186\\\",\\\"DB\\\":\\\"23.26\\\"},{\\\"TIME\\\":\\\"187\\\",\\\"DB\\\":\\\"29.38\\\"},{\\\"TIME\\\":\\\"188\\\",\\\"DB\\\":\\\"23.35\\\"},{\\\"TIME\\\":\\\"189\\\",\\\"DB\\\":\\\"36.26\\\"},{\\\"TIME\\\":\\\"190\\\",\\\"DB\\\":\\\"23.16\\\"},{\\\"TIME\\\":\\\"191\\\",\\\"DB\\\":\\\"23.41\\\"},{\\\"TIME\\\":\\\"192\\\",\\\"DB\\\":\\\"23.21\\\"},{\\\"TIME\\\":\\\"193\\\",\\\"DB\\\":\\\"35.99\\\"},{\\\"TIME\\\":\\\"194\\\",\\\"DB\\\":\\\"42.83\\\"},{\\\"TIME\\\":\\\"195\\\",\\\"DB\\\":\\\"23.14\\\"},{\\\"TIME\\\":\\\"196\\\",\\\"DB\\\":\\\"23.57\\\"},{\\\"TIME\\\":\\\"197\\\",\\\"DB\\\":\\\"23.68\\\"},{\\\"TIME\\\":\\\"198\\\",\\\"DB\\\":\\\"23.17\\\"},{\\\"TIME\\\":\\\"199\\\",\\\"DB\\\":\\\"23.28\\\"},{\\\"TIME\\\":\\\"200\\\",\\\"DB\\\":\\\"23.10\\\"},{\\\"TIME\\\":\\\"201\\\",\\\"DB\\\":\\\"23.46\\\"},{\\\"TIME\\\":\\\"202\\\",\\\"DB\\\":\\\"30.35\\\"},{\\\"TIME\\\":\\\"203\\\",\\\"DB\\\":\\\"23.79\\\"},{\\\"TIME\\\":\\\"204\\\",\\\"DB\\\":\\\"23.32\\\"},{\\\"TIME\\\":\\\"205\\\",\\\"DB\\\":\\\"23.84\\\"},{\\\"TIME\\\":\\\"206\\\",\\\"DB\\\":\\\"23.39\\\"},{\\\"TIME\\\":\\\"207\\\",\\\"DB\\\":\\\"24.21\\\"},{\\\"TIME\\\":\\\"208\\\",\\\"DB\\\":\\\"23.72\\\"},{\\\"TIME\\\":\\\"209\\\",\\\"DB\\\":\\\"25.38\\\"}]";

        JsonArray analysisRawDataArr = new JsonParser().parse(test).getAsJsonArray();
        JsonArray result = groupByMinites(analysisRawDataArr);
        System.out.println(result);
    }


    public static JsonArray groupByMinites(JsonArray aList) {
        JsonArray result = new JsonArray();
        aList.size();
        //리스트의값을 뽑는다
        //1~60초까지의 데이터중에 가장 큰 값을 가져온다.
        if(aList.size()>0){
            for(int i =0; i<aList.size(); i++) {
                JsonObject analysisRawData = (JsonObject) aList.get(i);
                //데이터 넣는 부분
                //타임은 recordStartDt 이후의 시간
                int time = analysisRawData.get("TIME").getAsInt();
                float db = analysisRawData.get("DB").getAsFloat();
                int minute = time / 60;
                //0에서는 무조건 기준값을 넣어줌
                if(i==0){
                    standardMinite =minute;
                    standardDb =db;
                }else{
                    //같으면 서로 비교
                    if(standardMinite==minute){
                        if(db>standardDb){
                            standardDb =db;
                        }
                        //하다가 끝이나면 값을 넣어줌
                        if(i==aList.size()){
                            JsonObject obj = new JsonObject();
                            obj.addProperty("TIME",minute);
                            obj.addProperty("DB",db);
                            result.add(obj);
                        }
                    }else{
                        JsonObject obj = new JsonObject();
                        obj.addProperty("TIME",minute);
                        obj.addProperty("DB",db);
                        result.add(obj);
                    }
                }
            }
        }
        return  result;
    }
}