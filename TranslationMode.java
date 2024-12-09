package com.example.Readable;

import static com.example.Readable.MainActivity.text;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class TranslationMode extends AppCompatActivity {

    static String tltext = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translationmode);

        final ListView lv1 = (ListView) findViewById(R.id.listView);//単語を表示するリスト
        lv1.setAdapter(MainActivity.adapter);

        //TextViewをスクロール可能に
        TextView tv1 = findViewById(R.id.tv1);
        TextView tv2 = findViewById(R.id.tv2);
        tv1.setMovementMethod(new ScrollingMovementMethod());
        tv2.setMovementMethod(new ScrollingMovementMethod());

        //リストのアイテムをクリック
        lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent subIntent = new Intent(getApplication(), TranslationActivity.class);
                TranslationActivity ta = new TranslationActivity(TranslationMode.this);
                tv1.setText(lv1.getItemAtPosition(position).toString());

                tltext = tv1.getText().toString();
//                        subIntent.putExtra("KEY_STRING", text);
                ta.execute(text);


            }
        });

        //戻る
        findViewById(R.id.backbtn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(TranslationMode.this, MainActivity.class);
                        startActivity(intent);
                    }
                }
        );
    }

}