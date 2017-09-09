package scot.mclarentech.magicmirror;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.johnhiott.darkskyandroidlib.ForecastApi;
import com.johnhiott.darkskyandroidlib.RequestBuilder;
import com.johnhiott.darkskyandroidlib.models.Request;
import com.johnhiott.darkskyandroidlib.models.WeatherResponse;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;
import static android.view.Gravity.CENTER;
import static scot.mclarentech.magicmirror.R.layout.activity_fullscreen;

public class FullscreenActivity extends AppCompatActivity {
    private static final boolean AUTO_HIDE = true;
    public static ListView m_listview;
    public static Activity myThis;
    public static TextView textViewIcon;
    public static TextView textViewWeather;
    public static TextView textViewLocation;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private Timer autoUpdate;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
    mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ForecastApi.create("a2473c26e33cdf595533164fc3e19824");
        setContentView(activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        m_listview = (ListView) findViewById(R.id.ListViewRight);
        textViewWeather = (TextView) findViewById(R.id.textViewWeather);
        textViewIcon = (TextView) findViewById(R.id.textViewIcon);
        textViewLocation = (TextView) findViewById(R.id.textViewLocation);

        myThis = this;

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        TextView btn = (TextView) findViewById(R.id.textViewIcon);
        btn.bringToFront();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TAP", "tap");
                Intent myIntent = new Intent(FullscreenActivity.this, SettingsActivity.class);
                FullscreenActivity.this.startActivity(myIntent);
            }
        });


        hide();
        doWeather();
        new getNews().execute("http://feeds.skynews.com/feeds/rss/home.xml");

        String[] values = new String[] { "", "" }; // Probably redundant

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.listview_row_layout,
                R.id.firstLine, values);
        m_listview.setAdapter(adapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void doWeather() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean display_weather = sharedPref.getBoolean("weather_switch", true);

        if (display_weather == true ) {
            updateWeather(); }
        else
        {
            clearWeather();
        }
    }

    private void clearWeather() {
        textViewWeather.setText("");

        Resources res = getResources();
        int resID = res.getIdentifier("" , "drawable", getPackageName());

        textViewLocation.setText("");

    }

    private void updateWeather(){

        RequestBuilder weather = new RequestBuilder();
        Request request = new Request();
        final String[] latLong = getLocation().split(",");

        request.setLat(latLong[0]);
        request.setLng(latLong[1]);
        request.setUnits(Request.Units.UK);
        request.setLanguage(Request.Language.ENGLISH);

        weather.getWeather(request, new Callback<WeatherResponse>() {
            @Override
            public void success(WeatherResponse weatherResponse, Response response) {

                String displayWeather = "";
                String icon_name = "";

                if (weatherResponse.getCurrently() != null) {
                    String temp = String.format("%.0f",weatherResponse.getCurrently().getTemperature()) +
                            "\u00B0C";
                    String loc = latLong[2] + ", " + latLong[3];

                    SpannableString temp_span =  new SpannableString(temp);
                    SpannableString loc_span = new SpannableString(loc);

                    temp_span.setSpan(new RelativeSizeSpan(1f), 0, temp.length(), SPAN_INCLUSIVE_INCLUSIVE);
                    loc_span.setSpan(new RelativeSizeSpan(0.5f), 0, loc.length(), SPAN_INCLUSIVE_INCLUSIVE);

                    CharSequence temp_loc = TextUtils.concat(temp_span, "\n", loc_span);

                    textViewIcon.setText(temp_loc);
                    textViewIcon.setGravity(CENTER);

                    displayWeather = ""; // weatherResponse.getCurrently().getSummary() +
                            // ", " + String.format("%.0f",weatherResponse.getCurrently().getTemperature()) +
                            // "\u00B0C\n\n";

                }

                if (weatherResponse.getMinutely() != null) {
                    displayWeather = displayWeather + weatherResponse.getMinutely().getSummary()
                            .replaceAll(".$","")+ "\n\n";
                    icon_name = weatherResponse.getMinutely().getIcon().replace("-", "_");
                }

                if (weatherResponse.getHourly() != null){
                    displayWeather = displayWeather + weatherResponse.getHourly().getSummary().replaceAll(".$","") + "\n\n";
                }

                if (weatherResponse.getDaily() != null) {
                    displayWeather = displayWeather + weatherResponse.getDaily().getSummary().replaceAll(".$","");
                    if (icon_name == "") {
                        icon_name = weatherResponse.getDaily().getIcon().replace("-", "_");
                    }
                }

                Log.d("","");
                TextView textViewWeather = (TextView) findViewById(R.id.textViewWeather);
                textViewWeather.setText(displayWeather);

                Log.d("****ICON****", icon_name);
                Resources res = getResources();
                int resID = res.getIdentifier(icon_name , "drawable", getPackageName());
                textViewIcon.setCompoundDrawablesWithIntrinsicBounds(resID, 0, 0, 0);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.d("******WEATHER*******", "Error while calling: " + retrofitError.getUrl());
                // TextView textViewWeather = (TextView) findViewById(R.id.textViewWeather);
                textViewWeather.setText("Error while calling: " + retrofitError.getUrl());
            }
        });

    }
    @Override
    public void onResume() {
        super.onResume();
        autoUpdate = new Timer();
        autoUpdate.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        doWeather();
                    }
                });
            }
        }, 0, 300000); // updates each 5 mins
    }

    @Override
    public void onPause() {
        autoUpdate.cancel();
        super.onPause();
    }

    private String getLocation() {
        double selectedLat = 0.0;
        double selectedLng = 0.0;
        String selectedLoc = "";
        String selectedCountry = "";
        String searchRoad = "London, UK";

        Geocoder g = new Geocoder(this);
        List<Address> addressList = null;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.contains("example_text")) {
            searchRoad = sharedPref.getString("example_text", "");
        }

        try {
            addressList = g.getFromLocationName(searchRoad, 1);

        } catch (IOException e) {
            Toast.makeText(this, "Location not found",     Toast.LENGTH_SHORT)
                    .show();
            e.printStackTrace();

        } finally {
            Address address = addressList.get(0);
            // TextView textViewLocation = (TextView) findViewById(R.id.textViewLocation);
            textViewLocation.setText(address.getLocality() + ", " + address.getCountryCode());

            if (address.hasLatitude() && address.hasLongitude()) {
                selectedLat = address.getLatitude();
                selectedLng = address.getLongitude();
                selectedLoc = address.getLocality();
                selectedCountry = address.getCountryCode();
            }
        }
        return String.format("%.3f", selectedLat) + "," +
                String.format("%.2f",selectedLng) + "," +
                selectedLoc + "," +
                selectedCountry;
    }

    public static void updateNews(String newsXML) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        InputStream inputStream = new ByteArrayInputStream(newsXML.getBytes());
        String[] headlines = new String[] { "", "", "", "", "", "", "", "", "", ""};
        try {
            org.w3c.dom.Document document = builder.parse(inputStream);
            Element docEle = document.getDocumentElement();
            NodeList nl = docEle.getChildNodes();

            if (nl != null) {
                int length = nl.getLength();
                int items = docEle.getElementsByTagName("item").getLength();
                if (items > 5) { items = 5; };

                for (int i = 0; i < length; i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) nl.item(i);
                        for (int j =0 ; j < items ; j++) {
                            headlines[j] = el.getElementsByTagName("item").item(j).getChildNodes().item(1).getTextContent();
                            if (Build.VERSION.SDK_INT >= 24) {
                                headlines[j] = Html.fromHtml(headlines[j], FROM_HTML_MODE_COMPACT).toString(); // for 24 api and more
                            } else {
                                headlines[j] = Html.fromHtml(headlines[j]).toString(); // or for older api
                            }

                            Log.d("item", headlines[j]);
                        }
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(myThis, R.layout.listview_row_layout,
                        R.id.firstLine, headlines);
                m_listview.setAdapter(adapter);

            }

        } catch (SAXException e) {
            e.printStackTrace();
            Log.d("d", "SAXException");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("d", "IOException");
        }
    }
}
