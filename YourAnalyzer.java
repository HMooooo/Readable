package com.example.Readable;

import android.hardware.camera2.CameraAccessException;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;

public interface YourAnalyzer {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void analyze(ImageProxy imageProxy) throws CameraAccessException;
}
