package scot.mclarentech.magicmirror;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextClock;
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;
import static scot.mclarentech.magicmirror.R.layout.activity_fullscreen;

public class FullscreenActivity extends AppCompatActivity {

    public ListView myListView;
    public Activity myThis;
    public TextView textViewIcon;
    public TextView textViewWeather;
    // private boolean mHasFocus;
    private static final int REQUEST_FINE_LOCATION = 0;
    private Timer autoUpdate;
    private View mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ForecastApi.create(getString(R.string.darksky_api_key));
        setContentView(activity_fullscreen);

        mContentView = findViewById(R.id.fullscreen_content);


        textViewWeather = (TextView) findViewById(R.id.textViewWeather);
        textViewIcon = (TextView) findViewById(R.id.textViewIcon);
        TextClock clk = (TextClock) findViewById(R.id.textClock);
        myThis = this;

        LinearLayout mLL_top = (LinearLayout) findViewById(R.id.ll_master);
        mLL_top.bringToFront();

        textViewIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(FullscreenActivity.this, SettingsActivity.class);
                FullscreenActivity.this.startActivity(myIntent);
            }
        });

        clk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(FullscreenActivity.this, SettingsActivity.class);
                FullscreenActivity.this.startActivity(myIntent);
            }
        });

        doWeather();
        doNews();

        String[] values = new String[]{"", ""}; // Probably redundant

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.listview_row_layout,
                R.id.firstLine, values);
        myListView = (ListView) findViewById(R.id.ListViewRight);
        myListView.setAdapter(adapter);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Boolean mHasFocus;
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

    private void doWeather() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean display_weather = sharedPref.getBoolean("weather_switch", true);

        if (display_weather) {
            updateWeather();
        } else {
            clearWeather();
        }
    }

    private void doNews() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean display_news = sharedPref.getBoolean("enable_news_reports", true);

        if (display_news) {
            String news_url = sharedPref.getString("rss_feed", "http://feeds.skynews.com/feeds/rss/home.xml");
            new getNews().execute(news_url);
        }
    }

    private void clearWeather() {
        textViewWeather.setText("");
    }

    private void updateWeather() {

        RequestBuilder weather = new RequestBuilder();
        Request request = new Request();
        String strLatLong = getLocation();

        if (strLatLong == null) {
            return;
        }

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
                    String temp = String.format(Locale.UK, "%.0f", weatherResponse.getCurrently().getTemperature()) +
                            "\u00B0C";
                    String loc = latLong[2] + ", " + latLong[3];

                    SpannableString temp_span = new SpannableString(temp);
                    SpannableString loc_span = new SpannableString(loc);

                    temp_span.setSpan(new RelativeSizeSpan(1f), 0, temp.length(), SPAN_INCLUSIVE_INCLUSIVE);
                    loc_span.setSpan(new RelativeSizeSpan(0.5f), 0, loc.length(), SPAN_INCLUSIVE_INCLUSIVE);

                    CharSequence temp_loc = TextUtils.concat(temp_span, "\n", loc_span);

                    textViewIcon.setText(temp_loc);
                    displayWeather = "";

                }

                if (weatherResponse.getMinutely() != null) {
                    displayWeather = displayWeather + weatherResponse.getMinutely().getSummary()
                            .replaceAll(".$", "") + "\n\n";
                    icon_name = weatherResponse.getMinutely().getIcon().replace("-", "_");
                }

                if (weatherResponse.getHourly() != null) {
                    displayWeather = displayWeather + weatherResponse.getHourly().getSummary().replaceAll(".$", "") + "\n\n";
                }

                if (weatherResponse.getDaily() != null) {
                    displayWeather = displayWeather + weatherResponse.getDaily().getSummary().replaceAll(".$", "");
                    if (icon_name.equals("")) {
                        icon_name = weatherResponse.getDaily().getIcon().replace("-", "_");
                    }
                }

                TextView textViewWeather = (TextView) findViewById(R.id.textViewWeather);
                textViewWeather.setText(displayWeather);
                Resources res = getResources();
                int resID = res.getIdentifier(icon_name, "drawable", getPackageName());
                textViewIcon.setCompoundDrawablesWithIntrinsicBounds(resID, 0, 0, 0);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                textViewWeather.setText(getString(R.string.weather_error));
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
                        doNews();
                    }
                });
            }
        }, 0, 300000); // update every 5 mins
    }

    @Override
    public void onPause() {
        autoUpdate.cancel();
        super.onPause();
    }

    private String getLocation() {
        double selectedLat = 0.0;
        double selectedLng = 0.0;
        boolean useManualLocation = true;

        String selectedLoc = "";
        String selectedCountry = "";
        String searchRoad = "London, UK";

        Geocoder g = new Geocoder(this);
        List<Address> addressList = null;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPref.contains("manual_location_switch")) {
            useManualLocation = sharedPref.getBoolean("manual_location_switch", false);
        }

        if (sharedPref.contains("example_text")) {
            searchRoad = sharedPref.getString("example_text", "");
        }

        if (useManualLocation) {
            // Manual location mode
            try {
                addressList = g.getFromLocationName(searchRoad, 1);

            } catch (IOException e) {
                Toast.makeText(this, getString(R.string.location_error), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {

                try {
                    Address address = addressList.get(0);
                    if (address.hasLatitude() && address.hasLongitude()) {
                        selectedLat = address.getLatitude();
                        selectedLng = address.getLongitude();
                        selectedLoc = address.getLocality();
                        if (selectedLoc == null) {
                            selectedLoc = address.getSubLocality();
                        }
                        if (selectedLoc == null) {
                            selectedLoc = address.getAdminArea();
                        }
                        selectedCountry = address.getCountryCode();
                    }
                } catch (Exception exc) {
                    Log.e("MM", "addressList exception");
                }
            }
        } else {
            // GPS mode
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestFineLocationPermission();
                    return null;
                }
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastLocation == null) {
                    // Revert to network provider if GPS fails
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastLocation != null) {
                    selectedLat = lastLocation.getLatitude();
                    selectedLng = lastLocation.getLongitude();
                    searchRoad = String.format(Locale.UK, "%.6f", lastLocation.getLatitude()) + ", " +
                            String.format(Locale.UK, "%.6f", lastLocation.getLongitude());

                    try {
                        addressList = g.getFromLocation(selectedLat, selectedLng, 1);
                        selectedLoc = addressList.get(0).getLocality();
                        if (selectedLoc == null) {
                            selectedLoc = addressList.get(0).getSubLocality();
                        }
                        if (selectedLoc == null) {
                            selectedLoc = addressList.get(0).getSubAdminArea();
                        }
                        selectedCountry = addressList.get(0).getCountryCode();
                    } catch (IOException e) {
                        Toast.makeText(this, getString(R.string.location_error), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        }
        if ((selectedLoc != null) && selectedLoc.equals("")) {
            selectedLoc = "Unknown";
        }

        if ((selectedCountry != null) && selectedCountry.equals("")) {
            selectedCountry = "Unknown";
        }

        return String.format(Locale.UK, "%.3f", selectedLat) + "," +
                String.format(Locale.UK, "%.2f", selectedLng) + "," +
                selectedLoc + "," +
                selectedCountry;
    }

    public void updateNews(String newsXML) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Integer maxItems = 5;

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        if (newsXML != null) {
            InputStream inputStream = new ByteArrayInputStream(newsXML.getBytes());
            String[] headlines = new String[]{"", "", "", "", "", "", "", "", "", ""};
            try {
                org.w3c.dom.Document document = builder.parse(inputStream);
                Element docEle = document.getDocumentElement();
                NodeList nl = docEle.getChildNodes();

                if (nl != null) {
                    int length = nl.getLength();
                    int items = docEle.getElementsByTagName("item").getLength();

                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(myThis);

                    if (sharedPref.contains("news_stories")) {
                        maxItems = Integer.parseInt(sharedPref.getString("news_stories", "5"));
                    }

                    if (items > maxItems) {
                        items = maxItems;
                    }

                    for (int i = 0; i < length; i++) {
                        if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                            Element el = (Element) nl.item(i);
                            for (int j = 0; j < items; j++) {
                                headlines[j] = el.getElementsByTagName("item").item(j).getChildNodes().item(1).getTextContent();
                                if (Build.VERSION.SDK_INT >= 24) {
                                    headlines[j] = Html.fromHtml(headlines[j], FROM_HTML_MODE_COMPACT).toString(); // for 24 api and more
                                } else {
                                    headlines[j] = Html.fromHtml(headlines[j]).toString(); // or for older api
                                }
                            }
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(myThis, R.layout.listview_row_layout,
                            R.id.firstLine, headlines);
                    myListView.setAdapter(adapter);

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

    private void requestFineLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            Snackbar.make(mContentView, R.string.permission_fine_location_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(FullscreenActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_FINE_LOCATION);
                        }
                    })
                    .show();
        } else {

            // Permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_FINE_LOCATION);
        }
    }

    private class getNews extends AsyncTask<String, Void, String> {
        String server_response;

        @Override
        protected String doInBackground(String... strings) {

            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    server_response = readStream(urlConnection.getInputStream());
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateNews(server_response);

        }

// Converting InputStream to String

        @NonNull
        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
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
}