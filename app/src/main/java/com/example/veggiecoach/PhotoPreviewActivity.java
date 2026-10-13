package com.example.veggiecoach;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class PhotoPreviewActivity extends AppCompatActivity {

    private ImageView imageViewPreview;
    private TextView textViewDescription;  // 텍스트뷰 변수 추가
    private TextView textViewDiseaseDescriptions;
    private String cropName;               // 전달받은 작물 이름

    //딥러닝
    private CucumGrowth cucumber;
    private CucumDisease cucumDisease;
    private StrawDisease strawDisease;
    private StrawGrowth strawberry;

    // 오이 생육 모델 - 지연 초기화
    private CucumGrowth getCucumberModel() {
        if (cucumber == null) {
            cucumber = new CucumGrowth(this);
        }
        return cucumber;
    }

    // 오이 병해 모델 - 지연 초기화
    private CucumDisease getCucumDiseaseModel() {
        if (cucumDisease == null) {
            cucumDisease = new CucumDisease(this);
        }
        return cucumDisease;
    }

    // 딸기 병해 모델 - 지연 초기화
    private StrawDisease getStrawDiseaseModel() {
        if (strawDisease == null) {
            strawDisease = new StrawDisease(this);
        }
        return strawDisease;
    }

    // 딸기 생육 모델 - 지연 초기화
    private StrawGrowth getStrawberryModel() {
        if (strawberry == null) {
            strawberry = new StrawGrowth(this);
        }
        return strawberry;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_preview);

        imageViewPreview = findViewById(R.id.imageViewPreview);
        textViewDescription = findViewById(R.id.textViewDescription);  // 텍스트뷰 연결
        textViewDiseaseDescriptions = findViewById(R.id.textViewDiseaseDescriptions);

        // 전달받은 작물명
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        cropName = prefs.getString("cropName", "작물 없음");

        String uriStr = getIntent().getStringExtra("imageUri");
        if (uriStr != null) {
            Uri imageUri = Uri.parse(uriStr);

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                //딥러닝 호출
                Deeplearning(bitmap);

                // ImageView에 Bitmap 이미지 출력
                imageViewPreview.setImageBitmap(bitmap);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "이미지 경로가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void Deeplearning(Bitmap bitmap) {
        // 1. 딥러닝 모델 입력 크기에 맞게 Bitmap 크기 조절 (224x224)
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        // 2. Bitmap을 딥러닝 모델 입력용 float 배열로 변환
        float[][][][] input = bitmapToInputArray(resizedBitmap);

        // 3. 작물 종류에 따른 딥러닝 모델 분류 수행
        if ("딸기".equals(cropName)) {
            String[] growthClassNames = {"초기", "중기", "성숙기", "과숙기", "기타"};
            String[] diseaseClassNames = {
                    "딸기 잎마름병 (Strawberry Leaf scorch)\n 잎이 마르고 변색되어 식물 생장이 저해됩니다.",
                    "건강한 딸기 (Strawberry healthy)\n 병징이 없고 정상적인 상태입니다.",
                    "흰가루병(과일) (Powdery mildew fruit)\n 과일 표면에 흰가루 같은 곰팡이가 생깁니다.",
                    "흰가루병(잎) (Powdery mildew leaf)\n 잎에 흰색 분말 모양의 곰팡이가 발생합니다.",
                    "탄저병 (Anthracnose)\n 잎과 줄기에 검은 반점이 생기고 괴사됩니다.",
                    "탄저병 과일썩음병 (Anthracnose fruit rot)\n 과일에 썩음이 진행되어 상품성이 떨어집니다.",
                    "잎반점병 (Leaf spot)\n 잎에 작은 반점이 나타나며 심할 경우 낙엽이 생깁니다.",
                    "앵귤러 리프스팟 (Angular leafspot)\n 잎 가장자리에 각진 반점이 생깁니다.",
                    "꽃마름병 (Blossom blight)\n 꽃이 마르고 떨어져 열매 형성이 어렵습니다.",
                    "버티실리움 시들음병 (Verticillium wilt)\n 뿌리 부위 병원균에 의해 식물이 시들고 죽습니다."
            };
            String[] diseaseTreatmentMethods = {
                    // 딸기 잎마름병 (Strawberry Leaf scorch)
                    "병든 잎은 제거하고, 과밀 식재를 피하며 통풍을 개선하세요. 필요 시 적절한 살균제를 사용합니다.",
                    // 건강한 딸기 (Strawberry healthy)
                    "별도의 대처법은 필요하지 않습니다. 병해 예방을 위해 정기적인 관리와 청결을 유지하세요.",
                    // 흰가루병(과일) (Powdery mildew fruit)
                    "감염 과일은 제거하고 통풍을 개선하세요. 유황계 살균제나 친환경 약제를 살포합니다.",
                    // 흰가루병(잎) (Powdery mildew leaf)
                    "잎에 환기가 잘되도록 하고, 저항성 품종을 선택하세요. 필요한 경우 살균제를 사용하세요.",
                    // 탄저병 (Anthracnose)
                    "감염 부위를 제거하고, 다우니, 벤레이트 등의 살균제를 사용합니다. 물빠짐이 좋은 토양에서 재배하세요.",
                    // 탄저병 과일썩음병 (Anthracnose fruit rot)
                    "감염 과일은 빨리 제거하고 과습 환경을 피하세요. 수확 전후 적절한 약제 살포가 필요합니다.",
                    // 잎반점병 (Leaf spot)
                    "병든 잎을 제거하고 살균제를 사용하세요. 비가 올 때 잎이 젖지 않도록 관리합니다.",
                    // 앵귤러 리프스팟 (Angular leafspot)
                    "세균성 병해이므로 구리제 살균제를 사용하고, 감염 식물은 제거합니다. 관수 시 잎에 직접 물이 닿지 않도록 합니다.",
                    // 꽃마름병 (Blossom blight)
                    "감염된 꽃은 제거하고 개화기 전후로 적절한 살균제를 사용하세요. 온실 내 습도 조절이 중요합니다.",
                    // 버티실리움 시들음병 (Verticillium wilt)
                    "감염 식물은 즉시 제거하고 윤작을 실시하세요. 토양을 태양열 소독하거나 저항성 품종을 선택합니다."
            };



            float[] result1 = getStrawberryModel().classify(input);
            float[] result2 = getStrawDiseaseModel().classify(input);

            int StGrowth_maxIndex = getMaxIndex(result1);
            int StDisease_maxIndex = getMaxIndex(result2);

            String resultText = cropName+"의 판별 결과";
            String DiseaseDescriptions = "성장 예측: " + growthClassNames[StGrowth_maxIndex] + "\n병해 예측: " + diseaseClassNames[StDisease_maxIndex]
                    +"\n\n대처방법: "+diseaseTreatmentMethods[StDisease_maxIndex];

            // 텍스트뷰에 이미지 경로 또는 원하는 텍스트 표시
            textViewDescription.setText(resultText);
            textViewDiseaseDescriptions.setText(DiseaseDescriptions);
        }

        if ("오이".equals(cropName)) {
            String[] growthClassNames = {
                    "개화기(Flowering)",       // 개화
                    "과실 맺힘(Fruiting)",        // 과실 맺힘
                    "과실 익음(Ripening)"         // 과실 익음
            };

            String[] diseaseClassNames = {
                    "탄저병 (Anthracnose)\n 곰팡이병으로 잎, 줄기, 과일에 흑갈색 병반이 생기며, 심하면 식물 전체가 말라 죽을 수 있습니다.",
                    "세균성 시들음병 (Bacterial Wilt)\n 박테리아에 의해 발생하며 식물이 갑자기 시들고 고사하는 증상을 보입니다.",
                    "배꼽 썩음병 (Belly Rot)\n 과일의 아래쪽이 물렁해지고 갈색 또는 검은 반점이 생기며 썩는 병입니다.",
                    "노균병 (Downy Mildew)\n 잎 뒷면에 회백색 곰팡이 포자가 생기고, 앞면에는 황색 병반이 나타납니다.",
                    "정상 오이 (Fresh Cucumber)\n 병해 없이 건강한 상태의 오이 과실입니다.",
                    "정상 잎 (Fresh Leaf)\n 병해 없이 정상적으로 자라고 있는 오이 잎입니다.",
                    "검은줄기썩음병 (Gummy Stem Blight)\n 줄기나 과일에 암갈색 병반과 끈적한 수액(검은 즙액)이 분비되는 곰팡이병입니다.",
                    "피튬 과실 부패병 (Pythium Fruit Rot)\n 토양성 병원균에 의해 과일이 무르고 썩으며, 수분을 많이 머금은 듯한 병반이 나타납니다."
            };

            String[] diseaseTreatmentMethods = {
                    // Anthracnose
                    "감염 부위를 제거하고, 벤레이트 또는 디티닐 등의 살균제를 주기적으로 살포합니다. 환기와 배수를 좋게 유지하세요.",
                    // Bacterial Wilt
                    "감염된 식물은 즉시 제거하고 소각하며, 오이벌레(박과잎벌레 등)를 방제해야 합니다. 저항성 품종을 선택하세요.",
                    // Belly Rot
                    "배수 상태를 개선하고 과습을 피하세요. 스트로베리 형식의 플라스틱 멀칭이나 지면에 직접 닿지 않게 재배합니다.",
                    // Downy Mildew
                    "병든 잎을 제거하고 메탈락실 계통의 살균제를 사용하세요. 환기와 일조량을 충분히 확보하세요.",
                    // Fresh Cucumber
                    "별도의 대처법은 필요하지 않습니다. 정상 상태를 유지하기 위해 적절한 물관리와 병해 예방을 지속하세요.",
                    // Fresh Leaf
                    "별도의 대처법은 필요하지 않습니다. 건강한 잎을 유지하려면 균형 잡힌 시비와 해충 관리를 병행하세요.",
                    // Gummy Stem Blight
                    "병든 줄기를 잘라내고 토양을 소독하거나 윤작합니다. 티오파네이트메틸(벤레이트) 계열의 살균제를 사용하세요.",
                    // Pythium Fruit Rot
                    "배수가 잘 되도록 토양을 관리하고, 고온다습한 환경을 피하세요. 예방적 토양 소독과 친환경 방제제 사용이 효과적입니다."
            };

            float[] result1 = getCucumberModel().classify(input);
            float[] result2 = getCucumDiseaseModel().classify(input);


            int CucumGrowth_maxIndex = getMaxIndex(result1);
            int CucumDisease_maxIndex = getMaxIndex(result2);

            String resultText = cropName+"의 판별 결과\n"+"성장 예측: ";
            String DiseaseDescriptions = "성장 예측: " + growthClassNames[CucumGrowth_maxIndex] + "\n병해 예측: " + diseaseClassNames[CucumDisease_maxIndex]
                    +"\n\n대처방법: "+diseaseTreatmentMethods[CucumDisease_maxIndex];;

            // 텍스트뷰에 이미지 경로 또는 원하는 텍스트 표시
            textViewDescription.setText(resultText);
            textViewDiseaseDescriptions.setText(DiseaseDescriptions);
        }
    }

    private int getMaxIndex(float[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx]) {
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    // Bitmap → float 배열 변환 (0~1로 정규화)
    private float[][][][] bitmapToInputArray(Bitmap bitmap) {
        //Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        float[][][][] input = new float[1][224][224][3];

        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int pixel = bitmap.getPixel(x, y);
                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f; // R
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // G
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;         // B
            }
        }
        return input;
    }

}
