package com.gachon.digital_photo_frame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.media.MediaPlayer;
import android.widget.VideoView;

import com.bumptech.glide.Glide;

import java.io.File;

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

        Uri defaultVideoURI = Uri.parse(defaultVideoURL);
        Uri defaultGifURI = Uri.parse(defaultGifURL);
        Uri defaultImageURI = Uri.parse(defaultImageURL);

        // 각 뷰 매핑
        videoView = findViewById(R.id.videoView);
        gifView = (ImageView)findViewById(R.id.gifView);
        imageView = (ImageView)findViewById(R.id.imageView);

        // 조건문으로 일차적으론 비지블로 설정
        videoView.setVisibility(View.INVISIBLE);
        gifView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.INVISIBLE);

        // 한 개씩 테스트 해본다
        // gifView.setVisibility(View.VI11SIBLE);
        // GifPlay();
        // imageView.setVisibility(View.VISIBLE);
        // ImagePlay();
        videoView.setVisibility(View.VISIBLE);
        VideoPlay();
    }

    public void VideoPlay() {
        videoURL = "/storage/emulated/0/uploads/sample.mp4";
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

    public void ImagePlay() {
        imageURL = "/storage/emulated/0/uploads/image.jpg";
        Uri imageURI = Uri.parse(imageURL);
        imageView.setImageURI(imageURI);
    }

    public void GifPlay() {
        gifURL = "/storage/emulated/0/uploads/gif_loop.gif";
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
}