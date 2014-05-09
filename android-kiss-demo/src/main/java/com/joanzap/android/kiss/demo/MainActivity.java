package com.joanzap.android.kiss.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;
import com.joanzap.android.kiss.api.annotation.InjectResponse;
import com.joanzap.android.kiss.api.annotation.InjectService;
import com.joanzap.android.kiss.api.internal.Kiss;
import com.joanzap.android.kiss.demo.event.UserEvent;

public class MainActivity extends ActionBarActivity {

    public static final String TAG = "MainActivity";

    @InjectService
    public DemoService service;

    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.text);
        Kiss.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        service.getUser(3L);
    }

    @InjectResponse
    public void onUserFetched(UserEvent e) {
        text.setText(e.name + " " + e.age);
    }

    @InjectResponse(UserEvent.class)
    public void onUserFetched() {
        Log.i(TAG, "User fetched !");
    }

}
