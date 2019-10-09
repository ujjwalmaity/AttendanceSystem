package dev.ujjwal.attendancesystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        button.setOnClickListener(this);
    }

    private void init() {
        button = findViewById(R.id.main_make_attendance);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_make_attendance:
                Intent in = new Intent(MainActivity.this, AttendanceActivity.class);
                startActivity(in);
                break;
        }
    }
}
