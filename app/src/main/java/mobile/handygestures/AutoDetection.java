package mobile.handygestures;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class AutoDetection implements Runnable {

    private Mat tmpImg;
    private int pred1, pred2;
    private int prediction1, prediction2;
    private TextView txtv;

    private long startTime;

    private List<String> labels, prevLabels;
    private List<Float> probs, prevProbs;

    private Recognition recognition;
    private Mat img;

    private ImageView imageView, imageView2;
    private TextView textView, textView1, textView2;

    final private Handler handler;
    private Preview preview;

    private Thread detector;

    public AutoDetection(Preview prv)
    {
        preview = prv;

        recognition = prv.recognition;
        img = preview.img;
        imageView = preview.imageView;
        imageView2 = preview.imageView2;
        textView = preview.textView;
        textView1 = preview.textView1;
        textView2 = preview.textView2;


        handler = new Handler();

        detector = new Thread(this);
    }

    public void begin() {
        if (detector != null) {
            detector.start();
        }
    }

    public void stop() {
        if (detector != null) {
            detector.interrupt();
        }
    }

    private int oneLetter(int num, final ImageView imgv) {

        while (true) {
            if (num == 2 && System.currentTimeMillis() - startTime > 5000) return -1;
            sleep(500);


            pred1 = classify();
            if (probs.size() == 0) Log.e ("my", "No");
            if (pred1 != -1 && detect()) {
                Log.e("my", "first");
                //resetView();

                sleep(300);


                tmpImg = img.clone();
                pred2 = classify();

                if (pred2 != -1 && detect()) {
                    Log.e("my", "tuuuuuuu");
                    //resetView();
                    Log.e("my", "is SECOND " + Integer.toString(compareLabels()));
                    if (compareLabels() < 500 && pred1 == pred2) {
                        handler.post(new Runnable() {
                            public void run() {
                                preview.setToBitmap(tmpImg, imgv);
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



    @Override
    public void run() {

        while (true) {
            txtv = textView1;
            prediction1 = oneLetter(1, imageView);
            sleep(250);
            txtv = textView2;
            startTime = System.currentTimeMillis();
            prediction2 = oneLetter(2, imageView2);
            if (prediction2 == -1) {
                resetInterface();
                continue;
            }

            handler.post(new Runnable() {
                public void run() {
                    recognition.runCommand(prediction1, prediction2);

                }
            });
            sleep(300);
            resetInterface();
           // resetView();

        }
    }


    private void resetInterface() {
        handler.post(new Runnable() {
            public void run() {
                imageView.setImageResource(R.drawable.img);
                imageView2.setImageResource(R.drawable.img);
                textView1.setText("");
                textView2.setText("");
            }
        });

    }

    private void resetView()
    {
        //preview.mOpenCvCameraView.disableView();

        handler.post(new Runnable() {
            public void run() {
                preview.mOpenCvCameraView.disableView();
                preview.mOpenCvCameraView.enableView();
//                preview.mOpenCvCameraView.setVisibility(SurfaceView.GONE);
//                preview.mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

            }
        });
        img = preview.img;

        //preview.mOpenCvCameraView.enableView();
        //img = null;
        //updateSortedLabels();
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
//        Log.e ("my", "detect");
        if (probs == null || probs.size() <= 0) return false;
        Log.e("my", "Best prob " + Float.toString(probs.get(probs.size() -1)));
        if(probs.get(probs.size() -1) > 0.6) return true;
        else return false;
    }


    private int classify() {
        int prediction = -1;
        if (img !=null && img.cols() > 0)
        {
            Bitmap bmp = getBitmapFromMat();
            prediction = recognition.classifyFrame(bmp);
            updateSortedLabels();
        }
        if (labels != null)
        {
            prevLabels = new ArrayList<String>(labels);
            prevProbs = new ArrayList<Float> (probs);
        }


        //if (prediction ==-1) Log.e ("my", "waht?");
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
}
