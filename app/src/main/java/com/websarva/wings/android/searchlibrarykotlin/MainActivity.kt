package com.websarva.wings.android.searchlibrarykotlin

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MainActivity : AppCompatActivity() {
    var book_text: EditText? = null
    var location_text:EditText? = null
    var isbn_text:EditText? = null
    private var session : String? = null
    var book_string: String? = null
    var isbn_string:String? = null
    var location_string:String? = null
    companion object {
        private const val DEBUG_TAG = "SearchTag"
        private const val URL = "https://api.calil.jp/check"
        private const val APP_ID = "ac8bea11c70423bc1a3b4b8fde42e19d"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        location_text = findViewById<EditText>(R.id.locationText)
        isbn_text = findViewById<EditText>(R.id.isbnText)
        book_text = findViewById<EditText>(R.id.bookText)
    }

    /* 取得するjsonデータの例
    {
         "session":"fa7ca42bb7aea9130f418653ffb09769",
         "continue": 0,
         "books": {
                     "9780307593313": {
                                        "Hyogo_Kobe": {
                                                        "status": "OK",
                                                        "libkey": {
                                                                        "中央図書館": "貸出可",
                                                                         "灘図書館": "貸出可"
                                                                   },
                                                        "reserveurl": "https://www.lib.city.kobe.jp/opac/opacs/find_detailbook?mode=one_line&type=PvolBook&kobeid=CT%3A7200120682&pvolid=PV%3A7200280596"
                                                       }
                                      }
                 }
     }
     */




    @UiThread
    private fun searchHttpRequest(urlFull: String) {
        lifecycleScope.launch {
            val result = BackgroundRunner(urlFull)
            PostRunner(result)
        }
    }

    /**
     * 非同期でお天気情報APIにアクセスするためのクラス。
     *
     * @param url お天気情報を取得するURL。
     */
    @WorkerThread
    private suspend fun BackgroundRunner(url: String): String {
        val returnVal = withContext(Dispatchers.IO) {
            var result = ""
            // URLオブジェクトを生成。
            val url = URL(url)
            // URLオブジェクトからHttpURLConnectionオブジェクトを取得。
            val con = url.openConnection() as? HttpURLConnection
            // conがnullじゃないならば…
            con?.let {
                try {
                    // 接続に使ってもよい時間を設定。
                    it.connectTimeout = 1000
                    // データ取得に使ってもよい時間。
                    it.readTimeout = 1000
                    // HTTP接続メソッドをGETに設定。
                    it.requestMethod = "GET"
                    // 接続。
                    it.connect()
                    // HttpURLConnectionオブジェクトからレスポンスデータを取得。
                    val stream = it.inputStream
                    // レスポンスデータであるInputStreamオブジェクトを文字列に変換。
                    result = is2String(stream)
                    Log.i("JSON_string", "jsonStr=$result")//取得は出来ている
                    // InputStreamオブジェクトを解放。
                    stream.close()
                }
                catch(ex: SocketTimeoutException) {
                    Log.w(DEBUG_TAG, "通信タイムアウト", ex)
                }
                // HttpURLConnectionオブジェクトを解放。
                it.disconnect()
            }
            result
        }
        return returnVal
    }

    @UiThread
    private fun PostRunner(result: String) {

        // ルートJSONオブジェクトを生成。
        val rootJSON = JSONObject(result)
        session = rootJSON.getString("session")
        val books: JSONObject = rootJSON.getJSONObject("books")
        val isbn = books.getJSONObject(isbn_string)
        val location = isbn.getJSONObject(location_string)
        val status = location.getString("status")
        if(status!="Error") {
            //在庫情報のURLを取得
            val reserveurl = location.getString("reserveurl")//ここでエラーメッセージ
            Log.i("reserveurl ", reserveurl)
            if (!(reserveurl == "")) {
                val uri = Uri.parse(reserveurl)
                //Intentで在庫情報のサイトへ
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }

    private fun is2String(stream: InputStream): String {
        val sb = StringBuilder()
        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        var line = reader.readLine()
        while(line != null) {
            sb.append(line)
            line = reader.readLine()
        }
        reader.close()
        return sb.toString()
    }

    //在庫検索ボタンを押したとき
    fun search(view: View?) {
        //ISBNの入力を取得
        isbn_string = isbn_text?.text.toString()
        Log.i("isbn_string", isbn_string!!)
        //地名の入力を取得
        location_string = location_text?.text.toString()
        val search_url =
            "$URL?appkey=$APP_ID&isbn=$isbn_string&systemid=$location_string&format=json&callback=no"
        if (!(isbn_string == "") && !(location_string == "")) {
            try {
                searchHttpRequest(search_url)
            } catch (e: Exception) {
                Log.e("Search_Error", e.message!!)
            }
        }
    }

    //再読み込みボタンを押したとき
    fun reload(view: View?) {
        val reload_url =
            "$URL?appkey=$APP_ID&session=$session&format=json&callback=no"
        try {
            searchHttpRequest(reload_url)
        } catch (e: Exception) {
            Log.e("Reload_Error", e.message!!)
        }
    }

    //ISBN検索ボタンを押したとき
    fun searchISBN(view: View?) {
        //書籍の名前を取得
        book_string = book_text!!.text.toString()
        if ((book_string == "") == false) {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.putExtra(SearchManager.QUERY, "$book_string ISBN")
            startActivity(intent)
        }
    }

}