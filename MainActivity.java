package com.manik.youtubeweb;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private WebView webView;
    private Switch adSwitch;
    private Switch backgroundSwitch;
    private SharedPreferences prefs;

    private String adScript = "";
    private ScriptHandler adScriptHandler;
    private boolean docStartSupported;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private static final Set<String> BLOCKED_HOSTS = new HashSet<>(Arrays.asList(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "securepubads.g.doubleclick.net"
    ));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        adScript = loadAsset("adblock.js");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(8), dp(14), dp(8));
        header.setBackgroundColor(Color.rgb(245, 245, 245));

        TextView title = new TextView(this);
        title.setText("YouTube Web");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));

        adSwitch = new Switch(this);
        adSwitch.setText("Ads Block");
        adSwitch.setTextSize(13);
        adSwitch.setTextColor(Color.BLACK);
        adSwitch.setChecked(prefs.getBoolean("adblock", true));
        header.addView(adSwitch);
        root.addView(header);

        LinearLayout bgRow = new LinearLayout(this);
        bgRow.setGravity(Gravity.CENTER_VERTICAL);
        bgRow.setPadding(dp(14), dp(4), dp(14), dp(4));
        bgRow.setBackgroundColor(Color.rgb(250, 250, 250));

        TextView bgLabel = new TextView(this);
        bgLabel.setText("Background Play");
        bgLabel.setTextSize(14);
        bgLabel.setTextColor(Color.BLACK);
        bgRow.addView(bgLabel, new LinearLayout.LayoutParams(0, dp(42), 1));

        backgroundSwitch = new Switch(this);
        backgroundSwitch.setChecked(prefs.getBoolean("background_play", false));
        bgRow.addView(backgroundSwitch);
        root.addView(bgRow);

        TextView note = new TextView(this);
        note.setText("Background Play ON করলে অ্যাপ ব্যাকগ্রাউন্ডে গেলে playback চালু রাখার চেষ্টা করবে। YouTube-এর সীমাবদ্ধতায় সবসময় কাজ নাও করতে পারে।");
        note.setTextSize(12);
        note.setTextColor(Color.DKGRAY);
        note.setPadding(dp(14), dp(6), dp(14), dp(8));
        root.addView(note);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);

        // Look like a normal mobile Chrome so Google sign-in is not rejected as "embedded WebView".
        String ua = settings.getUserAgentString();
        if (ua != null) {
            settings.setUserAgentString(ua.replace("; wv", "").replace("Version/4.0 ", ""));
        }
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        docStartSupported = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // Fallback for old WebView versions without document-start scripts.
                if (!docStartSupported && adSwitch.isChecked() && isYoutube(url)) {
                    view.evaluateJavascript(adScript, null);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!docStartSupported && adSwitch.isChecked() && isYoutube(url)) {
                    view.evaluateJavascript(adScript, null);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (adSwitch != null && adSwitch.isChecked() && isAdRequest(request.getUrl().toString())) {
                    return new WebResourceResponse(
                        "text/plain", "utf-8",
                        new ByteArrayInputStream(new byte[0])
                    );
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                return !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme));
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                ((FrameLayout) getWindow().getDecorView()).addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                hideSystemBars();
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }
        });

        root.addView(webView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));

        // Android 13+ requires runtime notification permission for the playback notification.
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 501);
        }

        setContentView(root);
        applyAdScript();

        adSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("adblock", checked).apply();
            applyAdScript();
            if (webView != null) webView.reload();
        });

        backgroundSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("background_play", checked).apply();
            if (checked) startBackgroundService();
            else stopBackgroundService();
        });

        // Restore the service if the switch was left ON.
        if (backgroundSwitch.isChecked()) startBackgroundService();

        webView.loadUrl("https://m.youtube.com");
    }

    /** Registers (or removes) the ad-block script that runs before YouTube's own scripts. */
    private void applyAdScript() {
        if (!docStartSupported || webView == null) return;
        try {
            if (adScriptHandler != null) {
                adScriptHandler.remove();
                adScriptHandler = null;
            }
            if (adSwitch.isChecked() && !adScript.isEmpty()) {
                Set<String> origins = new HashSet<>(Arrays.asList("https://*.youtube.com"));
                adScriptHandler = WebViewCompat.addDocumentStartJavaScript(webView, adScript, origins);
            }
        } catch (Exception ignored) {
            docStartSupported = false;
        }
    }

    private boolean isYoutube(String url) {
        if (url == null) return false;
        String host = android.net.Uri.parse(url).getHost();
        return host != null && (host.equals("youtube.com") || host.endsWith(".youtube.com"));
    }

    private String loadAsset(String name) {
        try (InputStream in = getAssets().open(name);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isAdRequest(String rawUrl) {
        try {
            android.net.Uri uri = android.net.Uri.parse(rawUrl);
            String host = uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            if (host != null) {
                host = host.toLowerCase(Locale.ROOT);
                for (String blocked : BLOCKED_HOSTS) {
                    if (host.equals(blocked) || host.endsWith("." + blocked)) return true;
                }
            }
            return path.contains("/pagead/") ||
                   path.contains("/ptracking") ||
                   path.contains("/api/stats/ads") ||
                   path.contains("/get_midroll_");
        } catch (Exception ignored) {
            return false;
        }
    }

    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void exitFullscreen() {
        if (customView == null) return;
        ((FrameLayout) getWindow().getDecorView()).removeView(customView);
        customView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void startBackgroundService() {
        Intent intent = new Intent(this, BackgroundPlaybackService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopBackgroundService() {
        // stopService avoids the "startForeground not called" crash on Android 8+.
        stopService(new Intent(this, BackgroundPlaybackService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The notification's Stop button changes the saved state; keep the switch in sync.
        if (backgroundSwitch != null) {
            boolean saved = prefs.getBoolean("background_play", false);
            if (backgroundSwitch.isChecked() != saved) {
                backgroundSwitch.setOnCheckedChangeListener(null);
                backgroundSwitch.setChecked(saved);
                backgroundSwitch.setOnCheckedChangeListener((button, checked) -> {
                    prefs.edit().putBoolean("background_play", checked).apply();
                    if (checked) startBackgroundService();
                    else stopBackgroundService();
                });
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush(); // keep the login after the app closes
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            exitFullscreen();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
