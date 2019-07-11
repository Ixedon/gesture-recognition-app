package mobile.handygestures;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE2 = 2;
    private Bitmap bitmap;
    private ImageView imageView, imageView2;

    private ImageClassifier classifier;
    private TextToSpeech t1;
    private char[] commandList;
    private char [] idToLetter;


    private TextView textView, textView1, textView2, textView3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, 250, 250);


        imageView =  findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap);

        imageView2 =  findViewById(R.id.imageView2);
        imageView2.setImageBitmap(bitmap);



        textView  =   findViewById(R.id.textView);
        textView1 =  findViewById(R.id.textView1);
        textView2 =  findViewById(R.id.textView2);
        textView3 =  findViewById(R.id.textView3);

        try {
            // create either a new ImageClassifierQuantizedMobileNet or an ImageClassifierFloatInception
            //      classifier = new ImageClassifierQuantizedMobileNet(getActivity());
            classifier = new ImageClassifierFloatInception(MainActivity.this);
        } catch (IOException e) {
            Log.e("my", "Failed to initialize an image classifier.", e);
        }


        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                    // t1.setPitch(1.0f);
                }
            }
        });

        commandList = new char[36];
        makeCommandList();
        idToLetter = new char []{'A','B','C','L','T','W'};

    }

    public void classify(View view)
    {
        int firstPrediction = classifyFrame(imageView);
        int secondPrediction = classifyFrame(imageView2);

        textView1.setText("" + idToLetter[firstPrediction]);// + " " +  Integer.toString(firstPrediction));
        textView2.setText("" + idToLetter[secondPrediction]);// + " " +  Integer.toString(secondPrediction));

        runCommand(firstPrediction, secondPrediction);


    }

    /** Classifies a frame from the preview stream. */
    private int classifyFrame(ImageView imgv) {
        if (classifier == null || MainActivity.this == null) {
            Log.e("my", "Uninitialized Classifier or invalid context.");
            return 0;
        }
        SpannableStringBuilder textToShow = new SpannableStringBuilder();

        Bitmap originalBitmap = ((BitmapDrawable) imgv.getDrawable()).getBitmap();

        Bitmap bitmap = ThumbnailUtils.extractThumbnail(originalBitmap, 224, 224);


        classifier.classifyFrame(bitmap, textToShow);
        bitmap.recycle();

        Log.e("my", "\n" + textToShow.toString());
        //Toast.makeText(this, textToShow.toString(), Toast.LENGTH_SHORT).show();



         return getPredictionID(textToShow.toString().charAt(0));



    }

    private void runCommand(int a, int b)
    {
        if (a >= 0 && b>= 0)
        {
            char command = getCommand(a, b);
            Log.e("my", "\ncomm: " + command);
            vibrate();
            executeCommand(command);
        }
    }

    private int getPredictionID(char c)
    {
        if (c == 'A') return 0;
        if (c == 'B') return 1;
        if (c == 'C') return 2;
        if (c == 'L') return 3;
        if (c == 'T') return 4;
        if (c == 'W') return 5;
        return 0;
    }



    private  void makeCommandList()
    {
        int indx = 0;
        for (int i=0;i<26;i++) commandList[indx++] = (char) (i + 65);
        for (int i=0;i<4;i++) commandList[indx++] = '#';
        commandList [indx++] = ' ';
        commandList [indx++] = '^';  // backspace
        commandList [indx++] = '%';  // clear
        commandList [indx++] = '@';  // speak
        for (int i=0;i<2;i++) commandList[indx++] = '#';
    }


    private char getCommand(int a, int b)
    {
        return commandList [a*6 + b];
    }


    private void executeCommand(char a)
    {

       String text = textView.getText().toString();

        if (a == '^' && text.length() > 0)
            textView.setText(text.substring(0, text.length() - 1));
        else if (a == '%')
            textView.setText("");
        else if (a == '@')
            speak(text);
        else  if (a != '#')
            putLetter(a);
    }

    public void speakButton(View view)
    {
        speak(textView.getText().toString());
    }

    private void speak (String toSpeak)
    {
        Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }



    public void pickImage(View view) {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);


        String name = view.getTag().toString();
        Log.e("my", "cos" + name);
        if (Integer.parseInt(name) == 1) startActivityForResult(intent, REQUEST_CODE);
        if (Integer.parseInt(name) == 2) startActivityForResult(intent, REQUEST_CODE2);


    }



    private  void vibrate()
    {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(150);
        }
    }


    private  void putLetter (char letter)
    {
        String oldText = textView.getText().toString();
        textView.setText(oldText + letter);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_CODE || requestCode == REQUEST_CODE2) && resultCode == Activity.RESULT_OK)
            try {
                // We need to recyle unused bitmaps
                if (bitmap != null) {
                    bitmap.recycle();
                }
                InputStream stream = getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream);
                Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap, 250, 250);
                stream.close();

                if (requestCode == REQUEST_CODE )  {Log.e("my", "first");  imageView.setImageBitmap(bitmap2);}
                if (requestCode == REQUEST_CODE2 ) {Log.e("my", "sec"); imageView2.setImageBitmap(bitmap2);}

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        super.onActivityResult(requestCode, resultCode, data);
    }

}


/*
Sign tree (# is inactive):

A A - A
A B - B
A C - C
A L - D
A T - E
A W - F

B A - G
B B - H
B C - I
B L - J
B T - K
B W - L

C A - M
C B - N
C C - O
C L - P
C T - Q
C W - R

L A - S
L B - T
L C - U
L L - V
L T - W
L W - X

L A - Y
L B - Z
L C - #
L L - #
L T - #
L W - #

L A - SPACE
L B - BACKSPACE
L C - CLEAR
L L - SPEAK
L T - #
L W - #

 */