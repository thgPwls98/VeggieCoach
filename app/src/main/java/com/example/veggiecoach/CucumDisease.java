package com.example.veggiecoach;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class CucumDisease {

    private Interpreter interpreter;


    // 생성자에서 Context 받는 버전 (일반적)
    public CucumDisease(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context.getAssets(), "cucumber_Disease.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
            // 필요하면 예외처리 추가
        }
    }

    // 기존 AssetManager 받는 생성자도 필요하면 유지 가능
    public CucumDisease(AssetManager assetManager) {
        try {
            interpreter = new Interpreter(loadModelFile(assetManager, "cucumber_Disease.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // 입력 타입은 모델에 맞게 수정하세요
    public float[] classify(float[][][][] inputData) {
        float[][] outputData = new float[1][8];  // 8은 클래스 개수 가정
        interpreter.run(inputData, outputData);
        return outputData[0];
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
