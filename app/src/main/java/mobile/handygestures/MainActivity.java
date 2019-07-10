package mobile.handygestures;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
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
    private Bitmap bitmap;
    private ImageView imageView;

    private ImageClassifier classifier;
    private TextToSpeech t1;
    private char[] commandList;

    private int lastPrediction = -1;
    private  int currentPrediction  = -1;
    private boolean first = true;

    private TextView textView, textView1, textView2, textView3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        imageView =  findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.img);

        textView =  findViewById(R.id.textView);
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

    }



    /** Classifies a frame from the preview stream. */
    public void classifyFrame(View View) {
        if (classifier == null || MainActivity.this == null) {
            Log.e("my", "Uninitialized Classifier or invalid context.");
            return;
        }
        SpannableStringBuilder textToShow = new SpannableStringBuilder();

        Bitmap originalBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        Bitmap bitmap = ThumbnailUtils.extractThumbnail(originalBitmap, 224, 224);

        lastPrediction = currentPrediction;

        // Bitmap bitmap = textureView.getBitmap(classifier.getImageSizeX(),
        // classifier.getImageSizeY());

        classifier.classifyFrame(bitmap, textToShow);
        bitmap.recycle();

        //Log.e("my", "\n" + textToShow.toString());
        Toast.makeText(this, textToShow.toString(), Toast.LENGTH_SHORT).show();

        currentPrediction = getPredictionID(textToShow.toString().charAt(0));

        if (!first)
        {
            textView3.setText("2");
            runCommand(lastPrediction, currentPrediction);
            first = true;
        }
        else
        {
            textView3.setText("1");
            first = false;
        }

        textView1.setText(Integer.toString(lastPrediction));
        textView2.setText(Integer.toString(currentPrediction));

    }

    private void runCommand(int a, int b)
    {
        if (a >= 0 && b>= 0)
        {
            char command = getCommand(a, b);
            Log.e("my", "\ncomm: " + command);
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

    public void pickImage(View View) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE);

    }


    private  void putLetter (char letter)
    {
       String oldText = textView.getText().toString();
       textView.setText(oldText + letter);
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