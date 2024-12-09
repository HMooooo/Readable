package com.example.Readable;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
//import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextToSpeech.OnInitListener {
    private TextureView textureView;//カメラを表示するビュー
    private final static int REQUEST_PERMISSION = 1002;
    private TextToSpeech textToSpeech;
    private RomajiHenkan henkan;
    static String text = "";//認識した文字
    MyGlobal myGlobal;
    static Boolean boo;//ローマ字モードかひらがなモードかを判別
    static ArrayList<String> data = new ArrayList<>();//TranslationModeで使用。単語のリスト
    static ArrayAdapter<String> adapter = null;//TranslationAModeで使用。単語のリスト

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureView);
        textureView.setVisibility(View.VISIBLE);
        TextView transralionText = findViewById(R.id.translation);
        TextView japanese = findViewById(R.id.japanese);
        textToSpeech = new TextToSpeech(this, this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        myGlobal = (MyGlobal) this.getApplication();

        //TextViewをスクロールできるようにする
        transralionText.setMovementMethod(new ScrollingMovementMethod());
        japanese.setMovementMethod(new ScrollingMovementMethod());


        //データが残っている場合は表示する
        if (!myGlobal.original.isEmpty()) {
            japanese.setText(myGlobal.original);
            transralionText.setText(myGlobal.trans);

            //カメラを閉じる
            findViewById(R.id.frameLayout1).setVisibility(View.GONE);
            findViewById(R.id.japanese).setVisibility(View.VISIBLE);
            findViewById(R.id.translation).setVisibility(View.VISIBLE);

            //音声ボタンと翻訳ボタンを有効化する
            ImageButton ttsButton = findViewById(R.id.spch);
            ImageButton intent = findViewById(R.id.intentBtn);
            ttsButton.setBackgroundResource(R.drawable.onsei);
            intent.setBackgroundResource(R.drawable.imi);
            findViewById(R.id.imageButton).setVisibility(View.GONE);
            findViewById(R.id.reload).setVisibility(View.VISIBLE);
        } else {
            //ボタンの無効化
            ImageButton ttsButton = findViewById(R.id.spch);
            ImageButton intent = findViewById(R.id.intentBtn);
            ttsButton.setEnabled(false);
            ttsButton.setOnClickListener(this);
            intent.setEnabled(false);

        }


        //ローマ字モード←→ひらがなモード
        ArrayAdapter<String> choose = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        choose.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        choose.add("Roman");
        choose.add("Hiragana");
        Spinner spinner = findViewById(R.id.spinner);
        spinner.setAdapter(choose);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //　アイテムが選択された場合
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                String item = (String) spinner.getSelectedItem();
                if (item.equals("Roman")) {
                    boo = true;//ローマ字モード
                } else {
                    boo = false;//ひらがなモード
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
                //　アイテムが選択されなかった場合
            }
        });


        //カメラのズームイン機能　https://abhiandroid.com/ui/zoomcontrols
        ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoom);
        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // calculate current scale x and y value of ImageView
                float x = textureView.getScaleX();
                float y = textureView.getScaleY();
                // set increased value of scale x and y to perform zoom in functionality
                textureView.setScaleX((float) (x + 1));
                textureView.setScaleY((float) (y + 1));
            }
        });
        //カメラのズームアウト機能
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // calculate current scale x and y value of ImageView
                float x = textureView.getScaleX();
                float y = textureView.getScaleY();
                // set decreased value of scale x and y to perform zoom out functionality
                textureView.setScaleX((float) (x - 1));
                textureView.setScaleY((float) (y - 1));
            }
        });


        //SDKを確認。23以上ならカメラ権限を許可してもらう。
        textureView.post((Runnable) (new Runnable() {
            public final void run() {
                if (Build.VERSION.SDK_INT >= 23) {
                    // permissionの確認
                    checkPermission();
                } else {
                    //カメラ起動
                    startCamera();
                }
            }
        }));

        //スマホの向きを検知して、写真の向きを変更する。
        textureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                updateTransform();
            }
        });

        //2画面目へインテント
        findViewById(R.id.intentBtn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (text.isEmpty()) {
                            Toast.makeText(MainActivity.this, "No data.", Toast.LENGTH_SHORT).show();
                        } else {
                            myGlobal.original = japanese.getText().toString();
                            myGlobal.trans = transralionText.getText().toString();
                            Intent intent = new Intent(MainActivity.this, TranslationMode.class);
                            startActivity(intent);
                        }
                    }
                }
        );

        //カメラを再度表示
        findViewById(R.id.reload).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        myGlobal.original = "";
                        myGlobal.trans = "";
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                }
        );


    }

    //カメラ権限が既に許可されているかを確認するメソッド。許可されている場合はそのままカメラを起動する。
    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            //カメラを起動する
            startCamera();
        } else {
            //カメラ権限の許可を要求する
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,},
                    REQUEST_PERMISSION);
        }
    }

    //requestPermissions()の結果が返ってくるメソッド
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();

            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    //許可されなかった場合。トーストを表示する。
                    Toast toast = Toast.makeText(this,
                            "If you don't allow, can't do anything.", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    // 拒否(次回から表示しない)を押された場合。トーストを表示する。
                    Toast toast = Toast.makeText(this, "If you don't allow, can't do anything.", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
    }


    //カメラを起動するメソッド
    private void startCamera() {
        PreviewConfig.Builder previewConfig = new PreviewConfig.Builder();
        Preview preview = new Preview(previewConfig.build());


        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);

                textureView.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();

            }
        });

        ImageCaptureConfig.Builder imageCaptureConfig = new ImageCaptureConfig.Builder();
        final ImageCapture imageCapture = new ImageCapture(imageCaptureConfig.build());
        ImageButton button = findViewById(R.id.imageButton);

        //撮影ボタンを押したときの処理
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ボタンの有効化
                ImageButton ttsButton = findViewById(R.id.spch);
                ImageButton intent = findViewById(R.id.intentBtn);
                ImageButton reload = findViewById(R.id.reload);
                findViewById(R.id.spch).setEnabled(true);
                ttsButton.setBackgroundResource(R.drawable.onsei);
                findViewById(R.id.intentBtn).setEnabled(true);
                intent.setBackgroundResource(R.drawable.imi);


                //写真を撮る
                imageCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                        //写真撮影が成功した場合
//                        ImageView imageView = findViewById(R.id.imageView);
//                        imageView.setImageBitmap(imageProxyToBitmap(image));
//                        imageView.setRotation(rotationDegrees);
//                        imageView.setScaleType(FIT_XY);
                        data.clear();

                        //非表示から表示、表示から非表示
                        findViewById(R.id.frameLayout1).setVisibility(View.GONE);
                        findViewById(R.id.japanese).setVisibility(View.VISIBLE);
                        findViewById(R.id.translation).setVisibility(View.VISIBLE);
                        reload.setVisibility(View.VISIBLE);

                        //文字認識をする。
                        Task t = recognizer.process(image.getImage(), 0);
                        t.addOnSuccessListener(
                                new OnSuccessListener() {
                                    //文字認識に成功した場合
                                    @Override
                                    public void onSuccess(Object o) {
                                        Log.e("log", ((Text) o).getText());
                                        Intent subIntent = new Intent(getApplication(), RomajiHenkan.class);
                                        henkan = new RomajiHenkan(MainActivity.this);
                                        text = ((Text) o).getText();
                                        henkan.execute(text);


                                        TextView ja = findViewById(R.id.japanese);
                                        ja.setText(text);


                                    }
                                }
                        ).addOnFailureListener(new OnFailureListener() {
                                                   //文字認識に失敗した場合
                                                   @Override
                                                   public void onFailure(@NonNull Exception e) {
                                                       Log.e("log", e.toString());
                                                   }
                                               }
                        );
                        image.close();
                    }
                });
            }
        });

        CameraX.bindToLifecycle(this, preview, imageCapture);
    }

    //文字を認識し、読みとるためのクラス
    TextRecognizer recognizer =
            TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());


    //スマホを横にしたときに写真の向きを横にするメソッド
    private void updateTransform() {
        Matrix matrix = new Matrix();

        float centerX = textureView.getWidth() / 2f;
        float centerY = textureView.getHeight() / 2f;

        float rotationDegrees = 0f;
        switch (textureView.getDisplay().getRotation()) {
            case Surface.ROTATION_0:
                rotationDegrees = 0f;
                break;

            case Surface.ROTATION_90:
                rotationDegrees = 90f;
                break;

            case Surface.ROTATION_180:
                rotationDegrees = 180f;
                break;

            case Surface.ROTATION_270:
                rotationDegrees = 270f;
                break;

            default:
                return;
        }

        matrix.postRotate(-rotationDegrees, centerX, centerY);

        textureView.setTransform(matrix);
    }


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }


    //回転角度を求めるメソッド
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, boolean isFrontFacing)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // Get the device's sensor orientation.
        CameraManager cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);

        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
        }
        return rotationCompensation;

    }


    //カメラIDをチェックするメソッド
    public List getBackCameraIds() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        ArrayList backIds = new ArrayList<>();
        try {
            String[] idList = cameraManager.getCameraIdList();
            for (String id : idList) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    backIds.add(id);
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return backIds;
    }


    //読み上げ
    public void onInit(int status) {
        // TTS初期化
        Locale locale = Locale.JAPAN;
        if (TextToSpeech.SUCCESS == status) {
            if (textToSpeech.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                textToSpeech.setLanguage(locale);
            } else {
                Log.d("debug", "initialized");
            }
        } else {
            Log.e("debug", "failed to initialize");
        }
    }

    //音声ボタンを押したときの処理
    @Override
    public void onClick(View v) {
        if (text.isEmpty()) {
            Toast.makeText(this, "No data.", Toast.LENGTH_SHORT).show();
        } else {
            speechText();
        }
    }

    private void shutDown() {
        if (null != textToSpeech) {
            // to release the resource of TextToSpeech
            textToSpeech.shutdown();
        }
    }

    private void speechText() {
        // EditTextからテキストを取得
        String string = henkan.hiragana;

        if (0 < string.length()) {
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop();
                return;
            }
            setSpeechRate();
            setSpeechPitch();

            textToSpeech.speak(string, TextToSpeech.QUEUE_FLUSH, null, "messageID");

            setTtsListener();
        }
    }

    // 読み上げのスピード
    private void setSpeechRate() {
        if (null != textToSpeech) {
            textToSpeech.setSpeechRate((float) 1.0);
        }
    }

    // 読み上げのピッチ
    private void setSpeechPitch() {
        if (null != textToSpeech) {
            textToSpeech.setPitch((float) 1.0);
        }
    }

    // 読み上げの始まりと終わりを取得
    private void setTtsListener() {
        int listenerResult =
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {
                        Log.d("debug", "progress on Done " + utteranceId);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.d("debug", "progress on Error " + utteranceId);
                    }

                    @Override
                    public void onStart(String utteranceId) {
                        Log.d("debug", "progress on Start " + utteranceId);
                    }
                });

        if (listenerResult != TextToSpeech.SUCCESS) {
            Log.e("debug", "failed to add utterance progress listener");
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        shutDown();
    }

}


