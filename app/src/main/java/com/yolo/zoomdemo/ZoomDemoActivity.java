package com.yolo.zoomdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yolo.zoomlayout.EHaiWidgetZoomLayout;

import java.util.ArrayList;
import java.util.List;
/**
 * @author: 37745
 * @date: 2020/4/9 13:40
 * @desc: 缩放的控件xml需设置 android:tag="zoom"
 */
public class ZoomDemoActivity extends AppCompatActivity {
    private EHaiWidgetZoomLayout zbv;
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
        rvAdapter.setOnItemClickListener(new RvAdapter.onItemClickListener() {
            @Override
            public void onClick(View v, int position) {
                Toast.makeText(ZoomDemoActivity.this, position + "", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initlist() {
        for (int i = 0; i < 10; i++) {
            list.add("填充数据" + i);
        }
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, ZoomDemoActivity.class);
        context.startActivity(starter);
    }


}
