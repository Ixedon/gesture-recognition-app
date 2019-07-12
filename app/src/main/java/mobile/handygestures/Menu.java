package mobile.handygestures;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Menu extends AppCompatActivity {

    Button galleryButton;
    Button helpButton;
    Button quitButton;
    Button previewButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        galleryButton = (Button) findViewById(R.id.button_gallery);
        galleryButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(Menu.this, Gallery.class));

            }
        });

        helpButton = (Button) findViewById(R.id.button_help);
        helpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(Menu.this, Help.class));

            }
        });

        quitButton = (Button) findViewById(R.id.button_quit);
        quitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
                System.exit(0);

            }
        });


        previewButton = (Button) findViewById(R.id.button_preview);
        previewButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(Menu.this, Preview.class));

            }
        });
    }
}
