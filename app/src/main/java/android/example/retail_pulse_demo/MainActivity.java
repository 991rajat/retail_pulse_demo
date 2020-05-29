package android.example.retail_pulse_demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_CODE = 100;
    private static final int PERMISSION_CODE = 101;
    private static final int RESET_VIEWS = 102;
    private static final String TAG = "------------>";
    private Button analyze;
    private ImageView imageView;
    String image_path = "";
    Boolean userSelectedImage = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        analyze = findViewById(R.id.buttonAnalyze);
        imageView = findViewById(R.id.imageView);

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
                    ;
                } else {
                    Toast.makeText(MainActivity.this, "No Image Selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

    // handling result of pick image and going back from result activity to main activity
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
                    // content resolver helps us to get the access of different content providers
                    Cursor cursor = this.getContentResolver().query(selectedImage, filePath, null, null, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        int columnIndexedValue = cursor.getColumnIndex(filePath[0]);
                        String picturePath = cursor.getString(columnIndexedValue);
                        cursor.close();
                        image_path = picturePath;
                        Log.e(TAG, "PIC PATH :" + picturePath);
                        //Glide.with(this).load(picturePath).into(imageView);
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
            bitmap = Bitmap.createScaledBitmap(bitmap,300,300,false);


            int width = bitmap.getWidth(); // 获取位图的宽
            int height = bitmap.getHeight(); // 获取位图的高

            int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组

            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            int alpha = 0xFF << 24;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int grey = pixels[width * i + j];

                    int red = ((grey & 0x00FF0000) >> 16);
                    int green = ((grey & 0x0000FF00) >> 8);
                    int blue = (grey & 0x000000FF);

                    grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                    grey = alpha | (grey << 16) | (grey << 8) | grey;
                    pixels[width * i + j] = grey;
                }
            }
            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            result.setPixels(pixels, 0, width, 0, 0, width, height);

        }
    }


}
