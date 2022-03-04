package scot.mclarentech.magicmirror;

// import android.webkit.WebView;

import java.io.BufferedReader;
// import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// import static scot.mclarentech.magicmirror.R.id.webView;

public class WeatherHttpClient {

    public String getWeatherData(String location, String owm_api_key) {
        String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?appid=" + owm_api_key + "&units=metric";
        HttpURLConnection con = null ;
        InputStream is = null;

        try {
            con = (HttpURLConnection) ( new URL(BASE_URL + location)).openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();

            // Let's read the response
            StringBuffer buffer = new StringBuffer();
            is = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line; // = null;
            while (  (line = br.readLine()) != null )
                buffer.append(line).append("\r\n");

            is.close();
            con.disconnect();
            return buffer.toString();
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
        finally {
            try { is.close(); } catch(Throwable t) {}
            try { con.disconnect(); } catch(Throwable t) {}
        }

        return null;

    }

}