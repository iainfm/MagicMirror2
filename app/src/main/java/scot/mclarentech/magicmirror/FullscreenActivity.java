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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
import static scot.mclarentech.magicmirror.R.layout.activity_fullscreen;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    public static ListView m_listview;
    public static Activity myThis;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private Timer autoUpdate;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
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
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
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
        // findViewById(R.id.imageView3).setOnTouchListener();

        ImageView btn = (ImageView) findViewById(R.id.imageView3);
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
        // Log.d("***NEWS***", newsXML);

        String[] values = new String[] { "FirstLine 1", "FirstLine 2" };
        // ListView m_listview = (ListView) findViewById(R.id.ListViewRight);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.listview_row_layout,
                R.id.firstLine, values);
        m_listview.setAdapter(adapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
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

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
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
        TextView textViewWeather = (TextView) findViewById(R.id.textViewWeather);
        textViewWeather.setText("");

        ImageView imageView3 = (ImageView) findViewById(R.id.imageView3);

        Resources res = getResources();
        int resID = res.getIdentifier("" , "drawable", getPackageName());
        imageView3.setImageResource(resID);

        TextView textViewLocation = (TextView) findViewById(R.id.textViewLocation);
        textViewLocation.setText("");
    }

    private void updateWeather(){
        RequestBuilder weather = new RequestBuilder();

        Request request = new Request();
        String[] latLong = getLocation().split(",");
        request.setLat(latLong[0]);
        request.setLng(latLong[1]);
        // request.setLat("55.944541");
        // request.setLng("-4.587783");
        request.setUnits(Request.Units.UK);
        request.setLanguage(Request.Language.ENGLISH);
        // request.addExcludeBlock(Request.Block.CURRENTLY);
        Log.d("BLOCK", latLong[0] + ", " + latLong[1]);
        weather.getWeather(request, new Callback<WeatherResponse>() {
            @Override
            public void success(WeatherResponse weatherResponse, Response response) {

                /* Log.d("******WEATHER*******", "Temp: " + weatherResponse.getCurrently().getTemperature());
                Log.d("****WEATHER****", weatherResponse.getDaily().getSummary());
                Log.d("****WEATHER****", weatherResponse.getHourly().getSummary());
                Log.d("****WEATHER****", "Temp: " + weatherResponse.getCurrently().getTemperature());
                Log.d("****WEATHER****", "Temp" + weatherResponse.getCurrently().getTemperature());
                Log.d("******WEATHER*******", "Summary: " + weatherResponse.getCurrently().getSummary().toString());
                Log.d("******WEATHER*******", "Hourly Sum: " + weatherResponse.getHourly().getSummary()); */
                String displayWeather = "";
                String icon_name = "";
                String temperature = "";

                if (weatherResponse.getCurrently() != null) {
                    displayWeather = weatherResponse.getCurrently().getSummary() +
                            ", " + String.format("%.0f",weatherResponse.getCurrently().getTemperature()) +
                            "\u00B0C\n\n";
                    if (icon_name == "") {
                        icon_name = weatherResponse.getCurrently().getIcon().replace("-", "_");
                    }
                }

                if (weatherResponse.getMinutely() != null) {
                    displayWeather = displayWeather + weatherResponse.getMinutely().getSummary() + "\n\n";
                    icon_name = weatherResponse.getMinutely().getIcon().replace("-", "_");
                }

                if (weatherResponse.getHourly() != null){
                    displayWeather = displayWeather + weatherResponse.getHourly().getSummary() + "\n\n";
                }

                if (weatherResponse.getDaily() != null) {
                    displayWeather = displayWeather + weatherResponse.getDaily().getSummary();
                    if (icon_name == "") {
                        icon_name = weatherResponse.getDaily().getIcon().replace("-", "_");
                    }
                }

                Log.d("","");
                TextView textViewWeather = (TextView) findViewById(R.id.textViewWeather);
                textViewWeather.setText(displayWeather);

                ImageView imageView3 = (ImageView) findViewById(R.id.imageView3);
                Log.d("****ICON****", icon_name);
                Resources res = getResources();
                int resID = res.getIdentifier(icon_name , "drawable", getPackageName());
                imageView3.setImageResource(resID);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.d("******WEATHER*******", "Error while calling: " + retrofitError.getUrl());
                TextView textViewWeather = (TextView) findViewById(R.id.textViewWeather);
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

        Geocoder g = new Geocoder(this);
        List<Address> addressList = null;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String searchRoad = sharedPref.getString("example_text", "");
        try {
            addressList = g.getFromLocationName(searchRoad, 1);

        } catch (IOException e) {
            Toast.makeText(this, "Location not found",     Toast.LENGTH_SHORT)
                    .show();
            e.printStackTrace();

        } finally {
            Address address = addressList.get(0);
            TextView textViewLocation = (TextView) findViewById(R.id.textViewLocation);
            textViewLocation.setText(address.getLocality() + ", " + address.getCountryCode());

            if (address.hasLatitude() && address.hasLongitude()) {
                selectedLat = address.getLatitude();
                selectedLng = address.getLongitude();
            }
        }
        return String.format("%.3f", selectedLat) + "," + String.format("%.2f",selectedLng);
    }

    public static void updateNews(String newsXML) {
        // ListView m_listview = (ListView) findViewById(R.id.ListViewRight);

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



            // ListView m_listview = (ListView) findViewById(R.id.ListViewRight);
            // ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.listview_row_layout,
                    // R.id.firstLine, values);
            // m_listview.setAdapter(adapter);

            if (nl != null) {
                int length = nl.getLength();
                for (int i = 0; i < length; i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) nl.item(i);
                        for (int j =0 ; j < el.getElementsByTagName("item").getLength() ; j++) {
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

                // ListView m_listview = (ListView) findViewById(R.id.ListViewRight);
                ;
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
