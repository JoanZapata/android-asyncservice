package com.joanzap.minim.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;
import com.joanzap.minim.api.annotation.InjectResponse;
import com.joanzap.minim.api.annotation.InjectService;
import com.joanzap.minim.api.internal.Minim;
import com.joanzap.minim.demo.event.UserEvent;

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

    @Override
    protected void onResume() {
        super.onResume();
        service.getUser(3L);
    }

    @InjectResponse
    public void onUserFetched(UserEvent e) {
        Log.i("MainActivity", "User fetched !");
        text.setText(e.name + " " + e.age);
    }

}
