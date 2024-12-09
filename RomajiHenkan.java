package com.example.Readable;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RomajiHenkan extends AsyncTask<String, String, String> {
    private Activity activity;
    String romaji = "";//変換後のローマ字文
    public String hiragana = "";//変換後のひらがな文


    public RomajiHenkan(Activity act) {
        this.activity = act;

    }

    protected void onPreExecute() {
        super.onPreExecute();
    }


    //非同期処理
    protected String doInBackground(String... params) {

        HttpURLConnection con = null;
        String urlStr = "https://jlp.yahooapis.jp/FuriganaService/V2/furigana"; //漢字交じり文→ひらがな文にするwebAPI
        String APPID = "[your APIkey]"; //YahooAPIに登録したID
        String regex_Japanise = "^[ぁ-ん]+$";//正規表現

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
            String body = "{\"id\":\"1234-1\", " +
                    "\"jsonrpc\":\"2.0\", " +
                    "\"method\":\"jlp.furiganaservice.furigana\", " +
                    "\"params\":{ " +
                    "\"q\":\"" + MainActivity.text.replace("·", "・") + " \" " +
                    "}" + "}";

            //header
            //データタイプをJSONに指定、ヘッダーにID情報を加える
            con.setRequestProperty("Content-type", "application/json");
            con.setRequestProperty("User-Agent", "Yahoo AppID: " + APPID);

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
                Log.e("log", "OK");
                InputStream in = con.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                //返ってきたJSONファイルをString型で
                String line;
                String readStr = "";
                while (null != (line = reader.readLine())) {
                    readStr += line;
                }
                Log.e("log", readStr);

                JSONObject jsonObject = new JSONObject(readStr);

                //Jsonからデータを取り出す
                TextView tv = this.activity.findViewById(R.id.translation);
                for (int i = 0; i < jsonObject.getJSONObject("result").getJSONArray("word").length(); i++) {
                    if (jsonObject.getJSONObject("result").getJSONArray("word").getJSONObject(i).has("roman")) {
                        romaji += jsonObject.getJSONObject("result").getJSONArray("word").getJSONObject(i).getString("roman") + " ";
                        hiragana += jsonObject.getJSONObject("result").getJSONArray("word").getJSONObject(i).getString("furigana") + " ";
                        if (!checkLogic(regex_Japanise, jsonObject.getJSONObject("result").getJSONArray("word").getJSONObject(i).getString("surface"))) {
                            MainActivity.data.add(jsonObject.getJSONObject("result").getJSONArray("word").getJSONObject(i).getString("surface"));
                        }
                    } else {
                        romaji += jsonObject.getJSONObject("result").getJSONArray("word").getJSONObject(i).getString("surface") + " ";
                        hiragana += jsonObject.getJSONObject("result").getJSONArray("word").getJSONObject(i).getString("surface") + " ";
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e("AST", "入出力処理の失敗", e);    /// JSON関連の処理失敗
        }

        //テキスト表示
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        TextView tv = this.activity.findViewById(R.id.translation);
        mainHandler.post(() -> {
            if (MainActivity.boo) {
                tv.setText(romaji);
            } else {
                tv.setText(hiragana);
            }

        });
        return romaji;
    }

    //正規表現
    public static boolean checkLogic(String regex, String target) {
        boolean result = true;
        if (target == null || target.isEmpty()) return false;

        Pattern p1 = Pattern.compile(regex);
        Matcher m1 = p1.matcher(target);
        result = m1.matches();
        return result;
    }


}
