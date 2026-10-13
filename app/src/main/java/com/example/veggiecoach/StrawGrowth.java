package com.example.veggiecoach;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class StrawGrowth {

    private Interpreter interpreter;

    private static final int numBoxes = 9;       // 박스 개수 (YOLO 모델에 맞게 수정)
    private static final int boxInfoSize = 1029;    // 박스 정보 크기 (x,y,w,h,conf + 클래스수 10)

    public StrawGrowth(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context.getAssets(), "2_yolov8_final_float16.tflite"));
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

    // classify 함수명 유지, YOLO 출력 후처리 후 성장 단계 인덱스 1차원 배열로 반환
    public float[] classify(float[][][][] inputData) {
        float[][][] yoloOutput = new float[1][numBoxes][boxInfoSize];
        interpreter.run(inputData, yoloOutput);

        // 박스별 confidence와 클래스 확률 곱으로 최고 점수인 클래스 인덱스 찾기
        float threshold = 0.5f;
        int classCount = boxInfoSize - 5;
        int bestClass = -1;
        float bestScore = 0f;

        for (int i = 0; i < numBoxes; i++) {
            float confidence = yoloOutput[0][i][4];
            if (confidence > threshold) {
                for (int c = 0; c < classCount; c++) {
                    float score = yoloOutput[0][i][5 + c];
                    float finalScore = confidence * score;
                    if (finalScore > bestScore) {
                        bestScore = finalScore;
                        bestClass = c;
                    }
                }
            }
        }

        // 결과를 float[]로 반환 (YOLO는 클래스 인덱스 1개만 필요하므로 배열 길이 1)
        float[] result = new float[classCount];
        for (int i = 0; i < classCount; i++) {
            result[i] = (i == bestClass) ? bestScore : 0f;
        }
        return result;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
