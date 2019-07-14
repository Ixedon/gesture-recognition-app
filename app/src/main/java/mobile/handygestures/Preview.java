package mobile.handygestures;


import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.format.Time;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;

import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;


public class Preview extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "my";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private int mCameraId = 1;

    private Mat img;
    private ImageView imageView, imageView2;
    private TextView textView, textView1, textView2;
    private Recognition recognition;

    private int firstPrediction, secondPrediction;

    private List<String> labels, prevLabels;
    private List<Float> probs, prevProbs;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Preview() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_preview);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.setCameraIndex(mCameraId);

        imageView  = findViewById(R.id.imageView5);
        imageView.setImageResource(R.drawable.img);

        imageView2  = findViewById(R.id.imageView4);
        imageView2.setImageResource(R.drawable.img);

        textView = findViewById(R.id.textView5);
        textView1 = findViewById(R.id.textView6);
        textView2 = findViewById(R.id.textView7);

        recognition = new Recognition(Preview.this, textView);

        final Handler handler = new Handler();



        new Thread(new Runnable() {

            private Mat tmpImg;
            private int pred1, pred2;
            private int prediction1, prediction2;
            private  TextView txtv;

            private long startTime;

            private int oneLetter(int num,final ImageView imgv ) {

                while (true)
                {
                    if (num == 2 && System.currentTimeMillis() - startTime > 5000) return -1;
                    sleep(400);

                    pred1 = classify();
                    if (detect()) {
                        Log.e("my", "first");
                        sleep(200);

                        tmpImg = img;
                        pred2 = classify();
                        if (detect()) {
                            Log.e(TAG, "is SECOND " + Integer.toString(compareLabels()));
                            if (compareLabels() < 300 && pred1 == pred2) {
                                handler.post(new Runnable() {
                                    public void run() {
                                        setToBitmap(tmpImg, imgv);
                                        txtv.setText("" + recognition.idToLetter[pred1]);
                                    }
                                });


                                recognition.vibrate(100);
                                return pred1;
                            }
                        }
                    }
                }
            }

            private void resetInterface()
            {
                handler.post(new Runnable() {
                    public void run() {
                        imageView.setImageResource(R.drawable.img);
                        imageView2.setImageResource(R.drawable.img);
                        textView1.setText("");
                        textView2.setText("");
                    }
                });

            }

            @Override
            public void run() {

                while (true)
                {

                    txtv = textView1;
                    prediction1 = oneLetter(1,imageView);
                    sleep(150);
                    txtv = textView2;
                    startTime = System.currentTimeMillis();
                    prediction2 = oneLetter(2,imageView2);
                    if (prediction2 == -1) {resetInterface(); continue;}

                    handler.post(new Runnable() {
                        public void run() {
                            recognition.runCommand(prediction1, prediction2);
                            sleep(150);

                        }
                    });

                    resetInterface();


                }
            }
        }).start();
    }

    private void sleep(int time)
    {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean detect()
    {
        //Log.e ("my", "detect");
        if (probs == null || probs.size() <= 0) return false;
//        Log.e("my", "Best prob " + Float.toString(probs.get(probs.size() -1)));
        if(probs.get(probs.size() -1) > 0.6) return true;
        else return false;
    }


    private int classify() {
        int prediction = 0;
        if (img !=null && img.cols() > 0)
        {
            Bitmap bmp = getBitmapFromMat();
           prediction = recognition.classifyFrame(bmp);
        }
        if (labels != null)
        {
            prevLabels = new ArrayList<String> (labels);
            prevProbs = new ArrayList<Float> (probs);
        }


        updateSortedLabels();
        //Log.e("my", "predict");
        return prediction;

    }

    private int compareLabels()
    {
        float sum = 0;

        final int size = prevLabels.size();
        for (int i = 0; i < size; i++)
        {
            sum += Math.abs(prevProbs.get(i) - probs.get(i));
            if (! prevLabels.get(i).equals(labels.get(i)) ) {sum += 1.0f;}
        }

        return  (int) (sum*100);

    }

    private Bitmap getBitmapFromMat()
    {
        Mat tmpImg = img;
        Core.rotate(tmpImg, tmpImg, Core.ROTATE_90_CLOCKWISE); //ROTATE_180 or ROTATE_90_
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(tmpImg.cols(), tmpImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmpImg, bmp);
        }
        catch (CvException e){Log.d("Exception",e.getMessage());}

        bmp = ThumbnailUtils.extractThumbnail(bmp, 100, 100);

      return bmp;
    }

    private void updateSortedLabels()
    {
        PriorityQueue<Map.Entry<String, Float>> sortedLabels = recognition.getClassifier().getLabelsList();

        if (sortedLabels == null) return;

        labels = new ArrayList<>();
        probs = new ArrayList<>();

        final int size = sortedLabels.size();
        //Log.e ("my", Integer.toString(size));
        for (int i = 0; i < size; i++) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            labels.add(label.getKey());
            probs.add(label.getValue());

            //Log.e("my", label.getKey() + " : " + Float.toString(label.getValue()) + "\n");
        }

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat tmpImg = inputFrame.rgba();

        if (mCameraId == 1) Core.flip(tmpImg, tmpImg, 1);
        img = tmpImg;
        return img;
    }

    private void setToBitmap(Mat tmpImg, ImageView imgv)
    {
        Core.rotate(tmpImg, tmpImg, Core.ROTATE_90_CLOCKWISE); //ROTATE_180 or ROTATE_90_
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(tmpImg.cols(), tmpImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmpImg, bmp);
        }
        catch (CvException e){Log.d("Exception",e.getMessage());}

        bmp = ThumbnailUtils.extractThumbnail(bmp, 100, 100);

        if (imgv != null) imgv.setImageBitmap(bmp);
    }

    public void showFrame(View view)
    {


        classifyFirst();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                classifySecond();
            }
        }, 2000);


    }

    private void classifyFirst() {
        setToBitmap(img, imageView);
        firstPrediction = recognition.classifyFrame(((BitmapDrawable) imageView.getDrawable()).getBitmap());
        textView1.setText("" + recognition.idToLetter[firstPrediction]);
        recognition.vibrate(100);
    }

    private void classifySecond() {
        setToBitmap(img, imageView2);
        secondPrediction = recognition.classifyFrame(((BitmapDrawable) imageView2.getDrawable()).getBitmap());
        Log.e("my", "f " + Integer.toString(firstPrediction) +" s " +  Integer.toString(secondPrediction));
        textView2.setText("" + recognition.idToLetter[secondPrediction]);
        recognition.vibrate(100);
        recognition.runCommand(firstPrediction, secondPrediction);


    }


    public void swapCamera(View view) {
        mCameraId = mCameraId^1; //bitwise not operation to flip 1 to 0 and vice versa
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }




}