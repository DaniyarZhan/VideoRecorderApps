package com.example.videorecorderapps;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.os.Build;
import androidx.activity.result.IntentSenderRequest;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private ActivityResultLauncher<Intent> videoCaptureLauncher;
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;
    private Uri videoUri;
    private Button buttonRecord;
    private Button buttonResume;
    private Button buttonPause;
    private Button buttonStop;
    private Button buttonDelete;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        buttonRecord = findViewById(R.id.buttonRecordVideo);
        buttonResume = findViewById(R.id.buttonResumeVideo);
        buttonPause = findViewById(R.id.buttonPauseVideo);
        buttonStop = findViewById(R.id.buttonStopVideo);
        buttonDelete = findViewById(R.id.buttonDeleteVideo);

        videoCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        videoUri = result.getData().getData();
                        if (videoUri != null) {
                            videoView.setVideoURI(videoUri);
                            videoView.start();
                            Toast.makeText(this, "Видео записано и воспроизводится",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(this, "Запись отменена",
                                Toast.LENGTH_SHORT).show();
                    }
                });
        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Пользователь нажал "Разрешить" в системном окне, файл удален
                        videoView.stopPlayback();
                        videoView.setVideoURI(null);
                        videoUri = null;

                        videoView.setVisibility(View.GONE);
                        videoView.setVisibility(View.VISIBLE);

                        Toast.makeText(this, "Видео удалено", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Удаление отменено", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        buttonRecord.setOnClickListener(v -> dispatchRecordVideoIntent());
        buttonResume.setOnClickListener(v -> dispatchWatchVideoIntent());
        buttonPause.setOnClickListener(v -> dispatchPauseVideoIntent());
        buttonStop.setOnClickListener(v -> dispatchStopVideoIntent());
        buttonDelete.setOnClickListener(v -> dispatchDeleteVideoIntent());
    }

    private void dispatchRecordVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            videoCaptureLauncher.launch(takeVideoIntent);
        }
        else {
            Toast.makeText(this, "На устройстве нет приложения камеры",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchWatchVideoIntent() {
        if (videoUri != null && !videoView.isPlaying()) {
            videoView.start();
        }
    }

    private void dispatchPauseVideoIntent() {
        if (videoView.isPlaying()) {
            videoView.pause();
        }
    }

    private void dispatchStopVideoIntent() {
        if (videoUri != null) {
            videoView.pause();
            videoView.seekTo(0);
        }
    }

    private void dispatchDeleteVideoIntent() {
        if (videoUri == null) {
            Toast.makeText(this, "Нет видео для удаления", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Пытаемся удалить файл напрямую (сработает на Android 9 и ниже)
            int rowsDeleted = getContentResolver().delete(videoUri, null, null);
            if (rowsDeleted > 0) {
                videoView.stopPlayback();
                videoView.setVideoURI(null);
                videoUri = null;
                videoView.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Видео удалено", Toast.LENGTH_SHORT).show();
            }
        }
        catch (SecurityException securityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Для Android 11 (API 30) и выше
                List<Uri> uriList = new ArrayList<>();
                uriList.add(videoUri);
                PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), uriList);
                IntentSenderRequest senderRequest = new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
                deleteRequestLauncher.launch(senderRequest);
            }
            else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // Для Android 10 (API 29)
                RecoverableSecurityException recoverableSecurityException = (RecoverableSecurityException) securityException;
                IntentSenderRequest senderRequest = new IntentSenderRequest.Builder(
                        recoverableSecurityException.getUserAction().getActionIntent().getIntentSender()
                ).build();
                deleteRequestLauncher.launch(senderRequest);
            }
        }
    }
}