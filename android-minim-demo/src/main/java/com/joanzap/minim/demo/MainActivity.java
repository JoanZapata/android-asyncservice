package com.joanzap.minim.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;
import com.joanzap.minim.api.annotation.InjectResponse;
import com.joanzap.minim.api.annotation.InjectService;
import com.joanzap.minim.demo.event.UserEvent;
import com.joanzap.minim.internal.Minim;

public class MainActivity extends ActionBarActivity {

    @InjectService
    public DemoService service;

    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.text);
        Minim.inject(this);
    }

    @InjectResponse
    public void onUserFetched(UserEvent e) {
        Log.i("MainActivity", "User fetched !");
        text.setText(e.name + " " + e.age);
    }

}
