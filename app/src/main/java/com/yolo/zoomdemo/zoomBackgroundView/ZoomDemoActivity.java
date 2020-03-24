package com.yolo.zoomdemo.zoomBackgroundView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yolo.zoomdemo.R;

import java.util.ArrayList;
import java.util.List;


public class ZoomDemoActivity extends AppCompatActivity {
    private ZoomBackgroundView zbv;
    private RecyclerView rv;
    private List<String> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom_demo);
        zbv = findViewById(R.id.zbv);
        rv = findViewById(R.id.rv);
        initlist();
        RvAdapter rvAdapter = new RvAdapter(this, list);
        rv.setAdapter(rvAdapter);
        rv.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initlist() {
        for (int i = 0; i < 20; i++) {
            list.add("填充数据");
        }
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, ZoomDemoActivity.class);
        context.startActivity(starter);
    }


}
