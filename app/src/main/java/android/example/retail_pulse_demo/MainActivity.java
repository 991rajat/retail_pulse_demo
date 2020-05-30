package android.example.retail_pulse_demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import org.tensorflow.lite.Interpreter;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_CODE = 100;
    private static final int PERMISSION_CODE = 101;
    private static final int RESET_VIEWS = 102;
    private static final String TAG = "------------>";
    private Button analyze;
    private ImageView imageView;
    Interpreter tflite;
    String image_path = "";
    Boolean userSelectedImage = false;
    private int DIM_IMG_SIZE_X = 300;
    private int DIM_IMG_SIZE_Y = 300;
    private int DIM_PIXEL_SIZE = 3;
    private int[] intValues;
    private ByteBuffer imgData = null;
    private float[][] output;
    private float[] floatValues;
    private int[] labelList;
    private double[][] vectorList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        analyze = findViewById(R.id.buttonAnalyze);
        imageView = findViewById(R.id.imageView);
        imgData = ByteBuffer.allocateDirect(4*DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        floatValues = new float[DIM_PIXEL_SIZE*DIM_IMG_SIZE_Y*DIM_IMG_SIZE_X*4];
        imgData.order(ByteOrder.nativeOrder());
        try {
            tflite = new Interpreter(loadModelFile());
            loadLabelsandVectors();
            Log.d(TAG, "onCreate: Model Loaded Successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.e("TAG", String.valueOf(userSelectedImage));
                //check runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        //permission not granted, requesting permission
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        //show popup for runtime permission
                        requestPermissions(permissions, PERMISSION_CODE);
                    } else {
                        //permission already granted
                        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, IMAGE_PICK_CODE);
                    }
                } else {
                    // system os is less than marshmallow
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, IMAGE_PICK_CODE);
                }
            }
        });


        analyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, String.valueOf(userSelectedImage));
                if (userSelectedImage) {
                    // Make Network Request
                    output = new float[1][16];
                    ScaleDown();
//                    String val = new String(imgData.array());
//                    Log.d(TAG, "onClick: "+Arrays.toString(imgData.array()));
//                    Log.d(TAG, "onClick: "+ Arrays.deepToString(output));
                    tflite.run(imgData,output);
                    Log.d(TAG, "onClick: "+ Arrays.deepToString(output));
                    compute();
                } else {
                    Toast.makeText(MainActivity.this, "No Image Selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void compute() {
        double Maxsum= Double.MAX_VALUE;
        int Index=-1;
        for(int i=0;i<32;i++)
        {
            double sum=0;
            for(int j=0;j<16;j++)
            {
                sum+=Math.pow((vectorList[i][j]-output[0][j]),2.0);
            }
            sum=Math.sqrt(sum);
            //Log.d(TAG, "Sum MAX: "+sum+" "+Maxsum);
            if(sum<Maxsum)
            {
                Maxsum=sum;
                Index = i;
                //Log.d(TAG, "compute: "+labelList[Index]);
            }
        }
        Log.d(TAG, "compute: "+labelList[Index]);
        String ans="";
        if(labelList[Index]==0)
            ans="Rock";
        else if(labelList[Index]==1)
            ans="Paper";
        else
            ans="Scissor";
        Toast.makeText(this, "Hand is "+ans, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, IMAGE_PICK_CODE);
                } else {
                    //permission denied
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case IMAGE_PICK_CODE: {

                    imageView.setImageURI(data.getData());
                    userSelectedImage = true;

                    Uri selectedImage = data.getData();

                    String[] filePath = {MediaStore.Images.Media.DATA};
                    Log.e(TAG, filePath.toString());

                    Cursor cursor = this.getContentResolver().query(selectedImage, filePath, null, null, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        int columnIndexedValue = cursor.getColumnIndex(filePath[0]);
                        String picturePath = cursor.getString(columnIndexedValue);
                        cursor.close();
                        image_path = picturePath;
                        Log.e(TAG, "PIC PATH :" + picturePath);

                     }
                    break;
                }
                case RESET_VIEWS: {
                    break;
                }
            }

        }
    }


    void ScaleDown()
    {
        if(userSelectedImage)
        {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(image_path,bmOptions);
            Bitmap result = getResizedBitmap(bitmap,DIM_IMG_SIZE_X,DIM_IMG_SIZE_Y);
            convertBitmapToByteBuffer(result);
        }
    }


    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("rock_paper_sci_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
//        int width = bm.getWidth();
//        int height = bm.getHeight();
//        float scaleWidth = ((float) newWidth) / width;
//        float scaleHeight = ((float) newHeight) / height;
//        Matrix matrix = new Matrix();
//        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, false);

        return resizedBitmap;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        Log.d(TAG, "getResizedBitmap: "+bitmap.getWidth());
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // loop through all pixels
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float

//                    Log.d(TAG, "convertBitmapToByteBuffer: "+(((val >> 16) & 0xFF) +" "+(((val >> 8) & 0xFF)) +" "+((val & 0xFF))));

                float red = (((val >> 16) & 0xFF))/255.0f;
                float green = (((val >> 8) & 0xFF))/255.0f;
                float blue = ((val & 0xFF))/255.0f;
                    //Log.d(TAG, "convertBitmapToByteBuffer: "+red +" "+green +" "+blue);
                    imgData.putFloat(red);
                    imgData.putFloat((green));
                    imgData.putFloat(blue);
            }
        }

    }

    void loadLabelsandVectors() throws IOException {
        labelList = new int[32];
        int k=0;
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(this.getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList[k]=Integer.parseInt(line.trim());
            //Log.d(TAG, "loadLabelsandVectors: "+labelList[k]);
            k++;
        }
        reader.close();

        vectorList = new double[32][16];
        k=0;
        BufferedReader reader2 =
                new BufferedReader(new InputStreamReader(this.getAssets().open("vectors.txt")));
        String line2;
        while((line2 = reader2.readLine())!=null)
        {
            String[] tokens = line2.trim().split("\\s+");
            for(int i=0;i<16;i++) {
                vectorList[k][i] = Double.parseDouble(tokens[i]);
                //Log.d(TAG, "loadLabelsandVectors: "+vectorList[k][i]);
            }
            k++;
        }

    }


}
