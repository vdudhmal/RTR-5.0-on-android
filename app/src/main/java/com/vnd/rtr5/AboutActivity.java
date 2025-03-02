package com.vnd.rtr5;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Button btnHome = findViewById(R.id.btnHome);
        Button btnAbout = findViewById(R.id.btnAbout);

        btnHome.setOnClickListener(view -> {
            finish();
        });

        btnAbout.setOnClickListener(view -> {
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.mipmap.ic_launcher);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        TextView aboutText = findViewById(R.id.aboutText);

        String aboutHtml = "This app demonstrates all <b>OpenGL programmable pipeline assignments</b> " +
                "done as part of <b>RTR(Real Time Rendering) 5.0 course</b> conducted by " +
                "<b>AstroMediComp(https://www.astromedicomp.org/)</b> from <b>April 2023 to October 2024</b>.<br/><br/>" +
                "🖥️ Technologies Used:<br/>" +
                "Programming Language: <b>Java</b><br/>" +
                "Rendering API: <b>OpenGL ES GLSL ES 3.20</b><br/>" +
                "Operating System: <b>Android</b><br/>" +
                "User Interface & Windowing: <b>Android SDK</b><br/><br/>" +
                "👩‍💻 Programming by <b>Vaishali Dudhmal</b>.<br/><br/>";

        Spanned formattedText = Html.fromHtml(aboutHtml, Html.FROM_HTML_MODE_COMPACT);
        aboutText.setText(formattedText);
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());
        aboutText.setLinkTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
    }
}
