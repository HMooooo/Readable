package com.example.Readable;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class TranslationActivity extends AsyncTask<String, String, String> {
    private Activity activity;
    String result = "";
    String get;

    public TranslationActivity(Activity act) {
        this.activity = act;
    }

    protected String doInBackground(String... params) {
        HttpURLConnection con = null;
        String getUrl = "https://api-free.deepl.com/v2/translate?auth_key=9f182fe5-da11-b539-5438-2f60d3b144c2:fx&text=" + MainActivity.text + "&target_lang=EN";
        String urlStr = "https://api-free.deepl.com/v2/translate";
        String apiKey = "[your APIkey]";

        try {
            URL url = new URL(urlStr);
            // 接続先URLへのコネクションを開く．まだ接続されていない
            con = (HttpURLConnection) url.openConnection();
            // HTTPメソッドをPOSTに指定
            con.setRequestMethod("POST");
            //HTTPリダイレクトを自動的に従わない
            con.setInstanceFollowRedirects(false);
            //レスポンスボディの受信を許可する（=入力可能）
            con.setDoInput(true);
            // リクエストボディの送信を許可する(=出力可能）
            con.setDoOutput(true);

            //リクエストボディ
            String body = "text=" + TranslationMode.tltext + "&source_lang=JA&target_lang=EN-US";
//            String body = "{\"source_lang\":\"JA\", " +
//                    "\"target_lang\":\"EN\", " +
//                    "\"text\":\"" + MainActivity.text + " \" " +
//                    "}";

            //header
            //データタイプをJSONに指定、ヘッダーにID情報を加える
            con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Authorization", "DeepL-Auth-Key [your APIkey]");
            con.setRequestProperty("User-Agent", "YourApp/1.2.3");
//            con.setRequestProperty("Content-Length", "56");

            //body
            byte[] outputInBytes = body.getBytes(StandardCharsets.UTF_8);
            // リクエストボディの書き込み
            OutputStream os = con.getOutputStream();
            os.write(outputInBytes);
            os.close();

            //接続・通信開始
            con.connect();

            //レスポンスコードを取得
            int status = con.getResponseCode();
            //レスポンスコードが"200"の時
            if (status == HttpURLConnection.HTTP_OK) {
                InputStream in = con.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                //返ってきたJSONファイルをString型に読み込む
                String line;
                String readStr = "";
                while (null != (line = reader.readLine())) {
                    readStr += line;
                }
                get = readStr;

                //JsonObjectにする
                JSONObject jsonObject = new JSONObject(readStr);

                //文字列を取り出す。romanがある場合はローマ字を、ない場合はそのままの文字列を取り出す。

                result += jsonObject.getJSONArray("translations").getJSONObject(0).getString("text");


            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e("AST", "入出力処理の失敗", e);    /// JSON関連の処理失敗
        }

        TextView tv2 = this.activity.findViewById(R.id.tv2);
        tv2.setText(result);
        return result;
    }
}

