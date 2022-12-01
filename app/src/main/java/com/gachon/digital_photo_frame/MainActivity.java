package com.gachon.digital_photo_frame;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.media.MediaPlayer;
import android.widget.VideoView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    public static String PACKAGE_NAME;

    private VideoView videoView; // 비디오를 실행 할 수 있게 도와주는 뷰
    private MediaController mediaController; // 재생이나 정지와 같은 미디어 제어 버튼부를 담당

    private ImageView gifView;

    private ImageView imageView;

    private String defaultVideoURL;
    private String defaultGifURL;
    private String defaultImageURL;

    private String videoURL;
    private String imageURL;
    private String gifURL;

    private int index = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) { // 앱이 첫 실행됬을 때 이곳을 수행한다. (이외에도 다른 라이프 사이클 메서드가 존재.)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 화면 꺼짐 방지 및 전체화면 유지, storage 접근 권한 설정
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        verifyStoragePermissions(this);

        // 기본 영상 경로 지정
        PACKAGE_NAME = getApplicationContext().getPackageName();
        defaultVideoURL = "android.resource://" + PACKAGE_NAME + "/" + R.raw.company_video;
        defaultGifURL = "android.resource://" + PACKAGE_NAME + "/" + R.raw.gif_loop;
        defaultImageURL = "android.resource://" + PACKAGE_NAME + "/" + R.raw.company_image;

        // 각 뷰 매핑
        videoView = findViewById(R.id.videoView);
        gifView = (ImageView)findViewById(R.id.gifView);
        imageView = (ImageView)findViewById(R.id.imageView);

        // 조건문으로 일차적으론 비지블로 설정
        videoView.setVisibility(View.INVISIBLE);
        gifView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.INVISIBLE);

        /*
        // 한 개씩 테스트 해본다
         gifView.setVisibility(View.VISIBLE);
         gifURL = "/storage/emulated/0/uploads/gif_loop.gif";
         GifPlay(gifURL);
         imageView.setVisibility(View.VISIBLE);
         imageURL = "/storage/emulated/0/uploads/image.jpg";
         ImagePlay(imageURL);
         videoView.setVisibility(View.VISIBLE);
         videoURL = "/storage/emulated/0/uploads/20221115/company_video.mp4";
         VideoPlay(videoURL);
         */
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            String jsonData = jsonRead();
            JSONObject jsonObject = new JSONObject(jsonData);
            String scheduleType = jsonObject.getString("schedule_type");
            JSONArray scheduleList = jsonObject.getJSONArray("schedule_list");

            ArrayList<HashMap> schedule = new ArrayList<>();

            for (int i = 0; i < scheduleList.length(); i++) {
                JSONObject scheduleObject = scheduleList.getJSONObject(i);

                String filePath = scheduleObject.getString("file_path");
                String fileExt = scheduleObject.getString("file_ext");
                String fileNameWithExt = scheduleObject.getString("file_name_with_ext");
                String duration = scheduleObject.getString("duration");
                String fileFullPath = filePath + fileNameWithExt;

                String[] durationArr = duration.split(":");
                int sec = 0;
                sec += Integer.parseInt(durationArr[0]) * 3600;
                sec += Integer.parseInt(durationArr[1]) * 60;
                sec += Integer.parseInt(durationArr[2]);
                String ms = Integer.toString(sec * 1000);

                HashMap<String, String> contentInfo = new HashMap<String, String>();
                contentInfo.put("fileExt", fileExt);
                contentInfo.put("fileFullPath", fileFullPath);
                contentInfo.put("ms", ms);

                schedule.add(contentInfo);
            }

            if (scheduleType.equals("repeat")) {
                startScheduleRepeat(schedule);
            } else if (scheduleType == "day") {
                // startScheduleDay(schedule);
            } else {
                startScheduleDefault();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void VideoPlay(String videoURL) {
        Uri videoURI = Uri.parse(videoURL);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView); // 미디어 컨트롤러에 미디어 뷰 셋팅

        // 해당 컨트롤러 등록을 삭제하면 videoView 컨트롤이 되지 않는다.
        // videoView.setMediaController(mediaController); // 미디어 뷰에 미디어 컨트롤러를 셋팅
        videoView.setVideoURI(videoURI); // 비디오 뷰의 주소를 설정
        videoView.requestFocus();

        // 리스너 등록
        videoView.setOnPreparedListener(mPrepare);
        videoView.setOnCompletionListener(mComplete);
    }

    public void ImagePlay(String imageURL) {
        Uri imageURI = Uri.parse(imageURL);
        imageView.setImageURI(imageURI);
    }

    public void GifPlay(String gifURL) {
        Uri gifURI = Uri.parse(gifURL);
        Glide.with(this)
            .load(new File(gifURI.getPath()))
            .into(gifView);
    }

    ///////// 하위 기타 설정 //////////

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    // 리스너를 통해 미디어 뷰의 라이플 사이클 관리를 할 수 있다.

    // 동영상이 끝났을 때,
    private MediaPlayer.OnCompletionListener mComplete = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            videoView.start();
        }
    };

    // 동영상이 준비 되었을 때,
    private MediaPlayer.OnPreparedListener mPrepare = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            videoView.start();
        }
    };

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public String jsonRead() {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/uploads/json";
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = new FileInputStream(new File(filePath, "repeat.json"));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();;
        }
        return sb.toString();
    }

    public void startScheduleRepeat(ArrayList schedule) {
        Log.d("schedule", schedule.toString());

        if (schedule.size() - 1 == index) {
            index = -1;
        }

        HashMap<String, String> scheduleObj;
        scheduleObj = (HashMap<String, String>) schedule.get(++index);
        int ms = Integer.parseInt(scheduleObj.get("ms").toString());

        playSchedule(scheduleObj);

        if (schedule.size() - 1 == index) {
            index = -1;
        }

        scheduleObj = (HashMap<String, String>) schedule.get(++index);
        ms = Integer.parseInt(scheduleObj.get("ms").toString());

        Intent mainActivity2Intent = new Intent(this, MainActivity2.class);
        mainActivity2Intent.putExtra("scheduleObj", scheduleObj);
        mainActivity2Intent.putExtra("ms", ms);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(mainActivity2Intent);
            }
        }, ms);

    }

    public void startScheduleDefault() {
        VideoPlay(defaultVideoURL);
        videoView.setVisibility(View.VISIBLE);
    }

    public void setInvisibleViews() {
        videoView.setVisibility(View.INVISIBLE);
        gifView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.INVISIBLE);
    }

    public void playSchedule(HashMap<String, String> scheduleObj) {
        Log.d("scheduleObj", scheduleObj.toString());

        String fileExt = scheduleObj.get("fileExt").toString();
        String fileFullPath = scheduleObj.get("fileFullPath").toString();

        setInvisibleViews();

        if(fileExt.equals(".mp4")) {
            videoView.setVisibility(View.VISIBLE);
            VideoPlay(fileFullPath);
        } else if(fileExt.equals(".gif")) {
            gifView.setVisibility(View.VISIBLE);
            GifPlay(fileFullPath);
        } else { // 사진
            imageView.setVisibility(View.VISIBLE);
            ImagePlay(fileFullPath);
        }

    }
}