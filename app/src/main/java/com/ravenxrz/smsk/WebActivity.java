package com.ravenxrz.smsk;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class WebActivity extends AppCompatActivity {

    private WebView webView;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        // 获取传入的关键字
        String keywork = getIntent().getStringExtra("keyword");

        // 设置回退按键
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // 初始化progressBar
        progressBar = findViewById(R.id.progress_bar);

        // webView及其相关设置
        webView = findViewById(R.id.web_view);
        WebSettings mWebSettings = webView.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSupportZoom(true);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setUseWideViewPort(true);

        webView.requestFocus();
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.loadUrl("https://www.baidu.com/s?wd="+keywork);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("使用其它浏览器");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "请选择浏览器"));
            } else {
                Toast.makeText(getApplicationContext(), "请下载浏览器", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 覆盖这个方法，使使用户的回退键不finish这个Acitivity
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK){
            webView.goBack();   //goBack()表示返回webView的上一页面
            return true;
        }else{
            // 不能goback,就直接finish
            finish();
        }
        return false;
    }


    class MyWebViewClient extends WebViewClient{

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }

    }


    class MyWebChromeClient extends WebChromeClient{
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            super.onProgressChanged(view, newProgress);
        }
    }
}
