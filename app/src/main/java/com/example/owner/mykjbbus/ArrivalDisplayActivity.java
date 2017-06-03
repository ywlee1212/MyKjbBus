package com.example.owner.mykjbbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ArrivalDisplayActivity extends AppCompatActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrival_display);
        listView = (ListView) findViewById(R.id.listView);

        Intent intent = getIntent();
        final ArrayList<MainActivity.ArrivalBus> arrivalBus = intent.getParcelableArrayListExtra("BusArrival");

        for(int i=0;i<arrivalBus.size();i++) {
//            Log.i("YWLEE", "ArrivalDisplayActivity: " + arrivalBus.get(i).getLINE_NAME());
        }

        ArrayAdapter<MainActivity.ArrivalBus> adapter=new ArrayAdapter<MainActivity.ArrivalBus>(this, android.R.layout.simple_list_item_1,android.R.id.text1, arrivalBus);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.i("YWLEE", "도착 버스 :"+arrivalBus.get(position).getLINE_NAME()+"  도착 시간 :" +arrivalBus.get(position).getREMAIN_MIN());
            }
        });
    }
}
