package com.joanzap.android.kiss.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;
import com.joanzap.android.kiss.api.annotation.Result;
import com.joanzap.android.kiss.api.annotation.InjectService;
import com.joanzap.android.kiss.api.internal.Kiss;
import com.joanzap.android.kiss.demo.event.UserEvent;

public class SecondActivity extends ActionBarActivity {

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

    @Result
    public void onUserFetched(UserEvent e) {
        Log.i("SecondActivity", "User fetched !");
        text.setText(e.name + " " + e.age);
    }

}
