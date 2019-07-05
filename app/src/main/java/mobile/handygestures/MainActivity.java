package mobile.handygestures;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;
    private Bitmap bitmap;
    private ImageView imageView;

    private ImageClassifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.img);



        try {
            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
            //      classifier = new ImageClassifierQuantizedMobileNet(getActivity());
            classifier = new ImageClassifierFloatInception(MainActivity.this);
        } catch (IOException e) {
            Log.e("my", "Failed to initialize an image classifier.", e);
        }

    }



    /** Classifies a frame from the preview stream. */
    public void classifyFrame(View View) {
        if (classifier == null || MainActivity.this == null) {
           // showToast("Uninitialized Classifier or invalid context.");
            Log.e("my","Uninitialized Classifier or invalid context." );
            return;
        }
        SpannableStringBuilder textToShow = new SpannableStringBuilder();

        Bitmap originalBitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();

        Bitmap bitmap = ThumbnailUtils.extractThumbnail(originalBitmap, 224, 224);

        //        Bitmap bitmap = textureView.getBitmap(classifier.getImageSizeX(),
        // classifier.getImageSizeY());
        classifier.classifyFrame(bitmap, textToShow);
        bitmap.recycle();

        Log.e("my", "\n" + textToShow.toString());
        Toast.makeText(this, textToShow.toString(), Toast.LENGTH_SHORT).show();

//        if (textToShow.toString().indexOf(":") != -1) {
//            String token = textToShow.toString().substring(0, textToShow.toString().indexOf(":"));
//            Activity activity = MainActivity.this;
//            activity.runOnUiThread(
//                    new Runnable() {
//                        @Override
//                        public void run() {
//                            highLightDirectionButton(token);
//                        }
//                    });
//        }
//
//        showToast(textToShow);
//        Log.e("text", textToShow.toString());
    }




    public void pickImage(View View) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK)
            try {
                // We need to recyle unused bitmaps
                if (bitmap != null) {
                    bitmap.recycle();
                }
                InputStream stream = getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream);
                stream.close();
                imageView.setImageBitmap(bitmap);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
