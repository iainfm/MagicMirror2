package scot.mclarentech.magicmirror;

// import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
// import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
// import android.location.Location;
// import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
// import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
// import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
// import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
// import androidx.core.app.ActivityCompat;

// import com.google.android.gms.location.FusedLocationProviderClient;
// import com.google.android.gms.location.LocationCallback;
// import com.google.android.gms.location.LocationRequest;
// import com.google.android.gms.location.LocationResult;
// import com.google.android.gms.location.LocationServices;
// import com.google.android.gms.tasks.OnCompleteListener;
// import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
// import com.johnhiott.darkskyandroidlib.BuildConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;
import static android.text.TextUtils.join;
import static scot.mclarentech.magicmirror.R.id.webView;
import static scot.mclarentech.magicmirror.R.id.webViewEE;
import static scot.mclarentech.magicmirror.R.layout.activity_fullscreen;

public class FullscreenActivity extends AppCompatActivity {

    private Activity myThis;
    private TextView textViewIcon;
    private TextView textViewWeather;
    private Timer autoUpdate;
    private WebView web;
    private WebView webEE;
    private WebView wIcon;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        LatLon.Lat = "";
        LatLon.Lon = "";
        super.onCreate(savedInstanceState);
        setContentView(activity_fullscreen);

        textViewWeather = findViewById(R.id.textViewWeather);
        textViewIcon = findViewById(R.id.textViewIcon);
        TextClock clk = findViewById(R.id.textClock);
        myThis = this;

        LinearLayout mLL_top = findViewById(R.id.ll_master);
        mLL_top.bringToFront();

        textViewIcon.setOnClickListener(v -> {
            Intent myIntent = new Intent(FullscreenActivity.this, SettingsActivity.class);
            FullscreenActivity.this.startActivity(myIntent);
        });

        clk.setOnClickListener(v -> {
            Intent myIntent = new Intent(FullscreenActivity.this, SettingsActivity.class);
            FullscreenActivity.this.startActivity(myIntent);
        });

        web = findViewById(webView);
        webEE = findViewById(R.id.webViewEE);
        webEE.getSettings().setJavaScriptEnabled(true); webEE.setBackgroundColor(Color.TRANSPARENT);
        wIcon = findViewById(R.id.webIcon);

        web.setWebViewClient(new MyBrowser());
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String owm_api_key= sharedPref.getString("api_key", "170e49c651266a64ef7fd5e526def9d3");
        if (owm_api_key.equals("6ac3fbdd607f7c7d086c01f5204bfcca")) {
            webEE.loadUrl("file:///android_asset/halloween62.html");
        }
        else {
            webEE.loadUrl("file:///android_asset/halloween.html");
        }

        checkScreenPinning();
        doNews();
        doWebPage();
        showSnackbar(getString(R.string.settings_tip));
    }

    private void showSnackbar(final String text) {
        View container = findViewById(R.id.fullscreen_content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        boolean mHasFocus;
        super.onWindowFocusChanged(hasFocus);
        mHasFocus = hasFocus;
        if (mHasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public static float pxFromDp(float dp, Context mContext) {
        return dp * mContext.getResources().getDisplayMetrics().density;
    }

    private void doWeatherNew() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean display_weather = sharedPref.getBoolean("weather_switch", true);

        if (!display_weather) {
            textViewWeather.setVisibility(View.INVISIBLE);
            clearWeather();
        } else {
            textViewWeather.setVisibility(View.VISIBLE);
            String manLoc; // = "";
            String city; // = "";

            manLoc = sharedPref.getString("manual_location", "Glasgow");
            city = "&q=" + manLoc;

            JSONWeatherTask task = new JSONWeatherTask();
            String owm_api_key; // = "";

            boolean use_own_api_key = sharedPref.getBoolean("custom_weather_api_key", false);
            if (use_own_api_key) {
                owm_api_key = sharedPref.getString("api_key", "170e49c651266a64ef7fd5e526def9d3");
            } else {
                owm_api_key = "170e49c651266a64ef7fd5e526def9d3";
            }
            task.execute(city, owm_api_key);
        }
    }

    private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {
        @Override
        protected Weather doInBackground(String... params) {
            Weather weather = new Weather();
            String data = ((new WeatherHttpClient()).getWeatherData(params[0], params[1]));

            try {
                if (data != null) {
                    weather = JSONWeatherParser.getWeather(data);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return weather;
        }

        @Override
        protected void onPostExecute(Weather weather) {
            String displayWeather;
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(myThis);
            final boolean display_units_in_degF = sharedPref.getBoolean("units_switch", false);
            final boolean bold_weather_text = sharedPref.getBoolean("bold_weather_text", false);
            final int weatherFontSize = Integer.parseInt(Objects.requireNonNull(sharedPref.getString("weather_fontsize", "4")));
            final boolean windspeed_in_kph = sharedPref.getBoolean("windspeed_switch", false);

            super.onPostExecute(weather);
            String weatherIcon = "werror";
            if (weather.currentCondition.getCondition() != null) {
                // int wID = weather.currentCondition.getWeatherId();
                String wIconId = weather.currentCondition.getIcon();

                weatherIcon = "werror";
                if (wIconId != null) {
                    /// debug: wIconId = "50d";
                    switch (wIconId) {
                        case "01d":
                            weatherIcon = "clear_day";
                            break;
                        case "01n":
                            weatherIcon = "clear_night";
                            break;
                        case "02d":
                            weatherIcon = "partly_cloudy_day";
                            break;
                        case "02n":
                            weatherIcon = "partly_cloudy_night";
                            break;
                        case "03d":
                        case "03n":
                            weatherIcon = "cloudy";
                            break;
                        case "04d":
                        case "04n":
                            weatherIcon = "very_cloudy";
                            break;
                        case "09d":
                        case "09n":
                            weatherIcon = "rain";
                            break;
                        case "10d":
                        case "10n":
                            weatherIcon = "heavy_rain";
                            break;
                        case "11d":
                        case "11n":
                            weatherIcon = "thunderstorm";
                            break;
                        case "13d":
                        case "13n":
                            weatherIcon = "snow";
                            break;
                        case "50d":
                        case "50n":
                            weatherIcon = "fog";
                            break;
                        default:
                            weatherIcon = "werror";
                            break;
                    }
                }

            }

            Resources res = getResources();
            int resID = res.getIdentifier(weatherIcon, "drawable", getPackageName());
            textViewIcon.setCompoundDrawablesWithIntrinsicBounds(resID, 0, 0, 0);

            TextView textViewWeather = findViewById(R.id.textViewWeather);
            textViewWeather.setTextSize((pxFromDp((3 * weatherFontSize), FullscreenActivity.this)));
            if (bold_weather_text) {
                textViewWeather.setTypeface(null, Typeface.BOLD);
            }
            else {
                textViewWeather.setTypeface(null, Typeface.NORMAL);
            }

            String temp; // = "";
            String loc = "";

            if (weather.currentCondition.getCondition() != null) {
                float current_temp = weather.temperature.getTemp();
                float feels_like = weather.temperature.getFeelsLike();
                float wSpeedms = weather.wind.getSpeed();
                double wSpeedmph = wSpeedms * 2.24;
                double wSpeedkph = wSpeedms * 3.6;
                float wDir = weather.wind.getDeg();

                String temp_units = "\u00B0";
                if (display_units_in_degF) {
                    current_temp = (current_temp * 9 / 5) + 32;
                    feels_like = (feels_like * 9 / 5) + 32;
                    temp_units += "F";
                } else {
                    temp_units += "C";
                }


                temp = String.format(Locale.UK, "%.0f", current_temp) + temp_units;
                String feels = String.format(Locale.UK, "%.0f", feels_like) + temp_units;
                String wSpeedMPH = String.format(Locale.UK, "%.0f", wSpeedmph);
                String wSpeedKPH = String.format(Locale.UK, "%.0f", wSpeedkph);
                String wSpeed = wSpeedMPH;
                String wUnits = " mph,";

                if (windspeed_in_kph) {
                    wSpeed = wSpeedKPH;
                    wUnits = " km/h,";
                }

                String wCity = weather.location.getCity();
                String wCountry = weather.location.getCountry();

                String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW", "N"};
                String wCondition = weather.currentCondition.getDescr();
                wCondition = wCondition.substring(0, 1).toUpperCase() + wCondition.substring(1).toLowerCase();
                double wIndex = (double) wDir % 360;
                wIndex = Math.round(wIndex / 22.5);
                String compassDir = directions[(int) wIndex];

                if (wCity != null) {
                    loc = weather.location.getCity();
                }
                if ((wCity != null) && (wCountry != null)) {
                    loc += ", " + weather.location.getCountry();
                }

                SpannableString temp_span = new SpannableString(temp);
                SpannableString loc_span = new SpannableString(loc);

                temp_span.setSpan(new RelativeSizeSpan(1f), 0, temp.length(), SPAN_INCLUSIVE_INCLUSIVE);
                loc_span.setSpan(new RelativeSizeSpan(0.5f), 0, loc.length(), SPAN_INCLUSIVE_INCLUSIVE);
                CharSequence temp_loc = TextUtils.concat(temp_span, "\n", loc_span);
                textViewIcon.setText(temp_loc);

                displayWeather = ""; //temp;
                displayWeather += "Feels like " + feels + "\n";
                displayWeather += wCondition + "\n";
                displayWeather += "Wind: " + wSpeed + wUnits + " " + compassDir + "\n";
            } else {
                displayWeather = getString(R.string.weather_error);
            }

            textViewWeather.setText(displayWeather);
        }
    }

    private void doNews() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean display_news = sharedPref.getBoolean("enable_news_reports", true);
        boolean custom_rss = sharedPref.getBoolean("custom_rss_feed_switch", false);
        TextView textViewNews = findViewById(R.id.textViewNews);
        String news_url;
        if (display_news) {
            textViewNews.setVisibility(View.VISIBLE);
            if (custom_rss) {
                news_url = sharedPref.getString("rss_url", "");
            } else {
                news_url = sharedPref.getString("rss_feed", "http://feeds.skynews.com/feeds/rss/home.xml");
            }
            new getNews().execute(news_url);
        } else {
            clearNews();
            textViewNews.setVisibility(View.VISIBLE);
        }

    }

    private void clearWeather() {
        textViewWeather.setText("");
    }

    private void clearNews() {
        TextView textViewNews = findViewById(R.id.textViewNews);
        textViewNews.setText("");
    }

    @Override
    public void onResume() {
        super.onResume();
        autoUpdate = new Timer();
        autoUpdate.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    doWeatherNew();
                    doNews();
                    doWebPage();
                    doEasterEgg();
                    checkScreenPinning();
                });
            }
        }, 0, 300000); // update every 5 mins (300000 ms)
    }

    @Override
    public void onPause() {
        autoUpdate.cancel();
        super.onPause();
    }

    public String getValue(Element item, String str) {
        NodeList n = item.getElementsByTagName(str);
        return this.getElementValue(n.item(0));
    }

    public final String getElementValue(Node elem) {
        Node child;
        if (elem != null) {
            if (elem.hasChildNodes()) {
                for (child = elem.getFirstChild(); child != null; child = child
                        .getNextSibling()) {
                    if (child.getNodeType() == Node.TEXT_NODE || (child.getNodeType() == Node.CDATA_SECTION_NODE)) {
                        return child.getNodeValue();
                    }
                }
            }
        }
        return "";
    }

    public void updateNews(String newsXML, Integer responseCode) {
        String TAG_CHANNEL = "channel";
        String TAG_TITLE = "title";
        String TAG_ITEM = "item";

        TextView textViewNews = findViewById(R.id.textViewNews);
        String joinedHeadlines;
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(myThis);
        int newsFontSize = Integer.parseInt(Objects.requireNonNull(sharedPref.getString("news_fontsize", "4")));
        final boolean bold_news_text = sharedPref.getBoolean("bold_news_text", false);
        int maxItems = 5;
        String[] headlines = new String[]{"", "", "", "", "", "", "", "", "", ""};

        if (sharedPref.contains("news_stories")) {
            maxItems = Integer.parseInt(Objects.requireNonNull(sharedPref.getString("news_stories", "5")));
        }
        if (newsXML != null && responseCode >= 0) {
            try {
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                // String A = String.valueOf(Html.fromHtml(newsXML));
                is.setCharacterStream(new StringReader(newsXML));
                doc = db.parse(is);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                Log.e("Error: ", Objects.requireNonNull(e.getMessage()));
            }

            try {
                NodeList nodeList = doc.getElementsByTagName(TAG_CHANNEL);
                Element e = (Element) nodeList.item(0);

                NodeList items = e.getElementsByTagName(TAG_ITEM);

                int newsItems = items.getLength();
                if (newsItems > maxItems) {
                    newsItems = maxItems;
                }
                for (int i = 0; i < newsItems; i++) {
                    Element e1 = (Element) items.item(i);

                    String title = this.getValue(e1, TAG_TITLE);
                    if (Build.VERSION.SDK_INT >= 24) {
                        title = Html.fromHtml(title, FROM_HTML_MODE_COMPACT).toString(); // for 24 api and more
                    } else {
                        title = Html.fromHtml(title).toString(); // or for older api
                    }

                    headlines[i] = title;
                }

                joinedHeadlines = join("\n", headlines);
                textViewNews.setTextSize((pxFromDp((3 * newsFontSize), FullscreenActivity.this)));
                if (bold_news_text) {
                    textViewNews.setTypeface(null, Typeface.BOLD);
                }
                else {
                    textViewNews.setTypeface(null, Typeface.NORMAL);
                }
                textViewNews.setText(joinedHeadlines);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            headlines[0] = getString(R.string.rss_error);
            headlines[1] = getString(R.string.rss_suggestion);
            headlines[2] = getString(R.string.rss_checkxport);
            joinedHeadlines = join("\n", headlines);
            textViewNews.setTextSize((pxFromDp((3 * newsFontSize), FullscreenActivity.this)));
            textViewNews.setText(joinedHeadlines);
        }
    }

     private class getNews extends AsyncTask<String, Void, String> {
        String server_response;
        int responseCode;

        @Override
        protected String doInBackground(String... strings) {

            try {
                URL url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    server_response = readStream(urlConnection.getInputStream());
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                server_response = "URL error downloading RSS feed";
                responseCode = -1;
            } catch (IOException e) {
                server_response = "IO error downloading RSS feed";
                responseCode = -2;
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateNews(server_response, responseCode);

        }

        @NonNull
        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line; // = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }
    }

    public void checkScreenPinning() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean pinScreen = sharedPref.getBoolean("pin_screen", false);
        ActivityManager activityManager;
        activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        if (pinScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (activityManager.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_PINNED) {
                        startLockTask();
                    }
            }
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                stopLockTask();
            }
        }
    }

    public void doEasterEgg() {
        Calendar calendar = Calendar.getInstance();
        int monthNumber = calendar.get(Calendar.MONTH) + 1;
        int dayNumber = calendar.get(Calendar.DAY_OF_MONTH);
        webEE = findViewById(webViewEE);
        if ((monthNumber == 10) && (dayNumber >= 21)) {
        // if (true) {
            Random r = new Random();
            int i = r.nextInt(4);
            Log.i("i=", String.valueOf(i));
            if (webEE.isShown() && (i != 0)) {
                webEE.setVisibility(View.INVISIBLE);
            } else {
                if (i == 0) { // i == 0
                    webEE.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void doWebPage() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean displayWebpage = sharedPref.getBoolean("display_webpage", false);
        String webPageURL = sharedPref.getString("display_webpage_url", "");
        int webPC = sharedPref.getInt("display_webpage_slider", 5) * 10;
        int wIconPC = 100 - webPC;

        LinearLayout.LayoutParams webLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, webPC);
        LinearLayout.LayoutParams wIconLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, wIconPC);
        web.setLayoutParams(webLP);
        wIcon.setLayoutParams(wIconLP);

        if (displayWebpage) {

            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setLoadWithOverviewMode(true);
            web.getSettings().setUseWideViewPort(true);
            // web.setInitialScale(100);
            web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            web.loadUrl(webPageURL);
            web.zoomOut();
            web.setVisibility(View.VISIBLE);
        }
        else {
            web.setVisibility(View.INVISIBLE);
            web.setLayoutParams(webLP);
            // web.setBackground(Color.RED);
            wIcon.setLayoutParams(wIconLP);
            // web.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

}