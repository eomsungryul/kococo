package kr.co.dwebss.kococo;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import kr.co.dwebss.kococo.http.ApiService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AddFirebaseStorageTest {
    Retrofit retrofit;
    ApiService apiService;
    String checkValueForAssertion = "";

    @Test
    public void AddFirebaseStorage() throws Exception {
        // 파이어베이스에서 에러가 날 수 있는 요인들
        // 1. mp3 파일이 없는 경우
        // 2. path가 이상할 경우
        // 3. 아이디가 맞지않는 경우
        // 4.


        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);
        //정상 파일
        String fileNm = "snoring-20190607_1002-07_1003_1559869391912.mp3";
        //파일이 없는 경우
//        String fileNm = "이상한 파일네임.mp3";

        //FindAppIdUtil fau = new FindAppIdUtil();
        String appId =  "9eba71d5-1e49-40e2-a9b1-525e8c45aa7d";

        //firebase 업로드 관련
        // 가장 먼저, FirebaseStorage 인스턴스를 생성한다
        // getInstance() 파라미터에 들어가는 값은 firebase console에서
        // storage를 추가하면 상단에 gs:// 로 시작하는 스킴을 확인할 수 있다
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://kococo-2996f.appspot.com/");

        //위에서 생성한 FirebaseStorage 를 참조하는 storage를 생성한다
        StorageReference storageRef = storage.getReference();
        // 위의 저장소를 참조하는 images폴더안의 space.jpg 파일명으로 지정하여
        // 하위 위치를 가리키는 참조를 만든다
        // 이부분은 firebase 스토리지 쪽에 업로드 되는 경로이다.
        //그렇기에 폴더 규칙을 재생데이터/앱아이디/일별날짜/파일 이런식으로 작성
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String uploadFirebasePath = "rec_data/"+appId+"/"+sdf.format(date)+"/"+fileNm;

        StorageReference spaceRef = storageRef.child(uploadFirebasePath);

        //내 실제 경로를 입력한다.
        File file = new File("/data/data/kr.co.dwebss.kococo/files/rec_data/9/"+fileNm);
        if(file.exists()){
            Uri putFile = Uri.fromFile(file);

            UploadTask uploadTask = spaceRef.putFile(putFile);

            System.out.println("==============AddFirebaseStorageTest======== start");
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    System.out.println("==============AddFirebaseStorageTest========실패"+exception.getMessage());
                    checkValueForAssertion = "fail";
                    assertEquals("success", checkValueForAssertion);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        System.out.println("============AddFirebaseStorageTest==========성공");
                    checkValueForAssertion = "success";
                    assertEquals("success", checkValueForAssertion);
                }
            });
        }else{
            //파일이 없는 경우
            System.out.println("==========AddFirebaseStorageTest============파일이 읍다 ");
            checkValueForAssertion = "fail";
            assertEquals("success", checkValueForAssertion);
    }

        //An unknown error occurred, please check the HTTP result code and inner exception for server response.


        //비동기 테스트는 어렵기 때문에 쓰레드를 잠시 멈추게 하던가 Awaitility라는 라이브러리를 사용하면된다고함
        //출처 : https://stackoverflow.com/questions/631598/how-to-use-junit-to-test-asynchronous-processes

        Thread.sleep(5000);

        System.out.println("==============AddFirebaseStorageTest======== end");


    }
}
