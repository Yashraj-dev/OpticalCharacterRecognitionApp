package com.example.opticalcharacterrecognitionapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
//import com.google.mlkit.vision.text.TextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageActivity;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class Home extends AppCompatActivity {
    private Button captureBtn,historyBtn;
    private Bitmap bitmap;
    private static final int REQUEST_CAMERA_CODE = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        captureBtn = findViewById(R.id.captureBtn);
        historyBtn = findViewById(R.id.historyBtn);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//            StrictMode.setVmPolicy(builder.build());
//        }

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    captureImage();
                }else{
                    requestPermission();
                }
            }
        });

        historyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent historyIntent = new Intent(Home.this,SavedDetailsActivity.class);
                startActivity(historyIntent);
            }
        });
    }

    private void captureImage(){
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(Home.this);
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(Home.this,new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_CODE);
    }

    private boolean checkPermission(){
        int cameraPermission = ContextCompat.checkSelfPermission(Home.this,Manifest.permission.CAMERA);
        return cameraPermission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0){
            boolean cameraPermissionGranted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            if(cameraPermissionGranted){
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
                captureImage();
            }
            else{
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            Toast.makeText(Home.this,"Camera",Toast.LENGTH_SHORT).show();
//            Log.i("CAMERA","Camera is working");
//            Bundle extras = data.getExtras();
//            bitmap = (Bitmap) extras.get("data");
//            detectText(bitmap);
//        }
//
//
//              if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
//                try {
//                    CropImage.ActivityResult result = CropImage.getActivityResult(data);
//                    if (resultCode == RESULT_OK) {
//                        Uri resultUri = result.getUri();
//                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
//                        detectText(bitmap);
//                    }
//                }catch (IOException e) {
//                        Toast.makeText(Home.this,"Some unknown Error",Toast.LENGTH_SHORT).show();
//                        e.printStackTrace();
//                    }
//            }
//            else{
//                Log.i("resultCode",String.valueOf(resultCode));
//            }
//    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(Home.this, "Camera", Toast.LENGTH_SHORT).show();
            Log.i("CAMERA", "Camera is working");
            Bundle extras = data.getExtras();
            bitmap = (Bitmap) extras.get("data");
            detectText(bitmap);
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    detectText(bitmap);
                } catch (IOException e) {
                    Toast.makeText(Home.this, "Error loading cropped image", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(Home.this, "Crop error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.i("resultCode", String.valueOf(resultCode));
        }
    }

//    private void getTextFromImage(Bitmap bitmap){
//        TextRecognizer recognizer = new TextRecognizerOptions.Builder(this).build();
//    }

    private void detectText(Bitmap bitmap){
        Log.i("1","Inside detectText");
        InputImage image = InputImage.fromBitmap(this.bitmap,0);
        Log.i("2","After Input image");
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result = recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(@NonNull Text text) {
                String blockText = "";
                String tempBlockText = "";
                StringBuilder result = new StringBuilder();
                try {
                    for(Text.TextBlock block: text.getTextBlocks()){
                        blockText = block.getText();
                        Point[] blockCornerPoint = block.getCornerPoints();
                        Rect blockFrame = block.getBoundingBox();

                        Log.i("mid0","checking");
                        for(Text.Line line: block.getLines()){
                            Log.i("mid1","checking");

                            String lineText = line.getText();
                            Point[] linearCornerPoint = line.getCornerPoints();
                            Rect linRect = line.getBoundingBox();
                            for(Text.Element element: line.getElements()){
                                Log.i("mid2","checking");

                                String elementText = element.getText();
                                result.append(elementText);
                            }
                            result.append("\n");
                        }
                        tempBlockText = tempBlockText +blockText+"\n";
                        result.append("\n");
                    }
                    Intent myIntent = new Intent(Home.this, ResultActivity.class);
                    myIntent.putExtra("recognizedText", tempBlockText);

                    ByteArrayOutputStream _bs = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, _bs);
                    myIntent.putExtra("byteArray", _bs.toByteArray());
                    startActivity(myIntent);

                    myIntent.putExtra("capturedImage",bitmap);
                    Log.i("IResult","After  adding Bitmap before start");
                    startActivity(myIntent);
                    Log.i("IResult1","After Start");
                }catch (Exception e){
                    Log.i("exp",e.toString());
                    Toast.makeText(Home.this,"Failed to detect text from Image",Toast.LENGTH_SHORT).show();
                }


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Home.this,"Failed to detect text from Image",Toast.LENGTH_SHORT).show();
            }
        });
    }

}




