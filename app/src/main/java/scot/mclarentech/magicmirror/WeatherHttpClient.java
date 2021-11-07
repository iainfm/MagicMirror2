package scot.mclarentech.magicmirror;

import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static scot.mclarentech.magicmirror.R.id.webView;

public class WeatherHttpClient {
// https://api.openweathermap.org/data/2.5/weather?appid=170e49c651266a64ef7fd5e526def9d3&units=metric&lat=55.9&lon=-4.5
    // http://api.openweathermap.org/data/2.5/forecast?appid=170e49c651266a64ef7fd5e526def9d3&units=metric&lat=56&lon=-4.6&cnt=3&mode=xml
    // private static String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?appid=" + "170e49c651266a64ef7fd5e526def9d3" + "&units=metric&q=";;
    // 170e49c651266a64ef7fd5e526def9d3
    // private static String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?appid=170e49c651266a64ef7fd5e526def9d3&units=metric&lat=56&lon=-4.6&cnt=3&q=";
    private static String IMG_URL = "http://openweathermap.org/img/wn/";
    private static String IMG_SUFFIX = ".png/";


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
            String line = null;
            while (  (line = br.readLine()) != null )
                buffer.append(line + "\r\n");

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

    public byte[] getImage(String code) {
        HttpURLConnection con = null ;
        InputStream is = null;
        try {
            // URL url = new URL(IMG_URL + code + IMG_SUFFIX);
            URL url = new URL("https://api.openweathermap.org/data/2.5/weather?appid=170e49c651266a64ef7fd5e526def9d3&units=metric&q=glasgow");
            // URL url = new URL("https://api.openweathermap.org/data/2.5/onecall?appid=170e49c651266a64ef7fd5e526def9d3&units=metric&lat=55.9&lon=-4.5");
            con = (HttpURLConnection) url.openConnection();

            // con = (HttpURLConnection) ( new URL(IMG_URL + code + IMG_SUFFIX)).openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();

            // Let's read the response
            is = con.getInputStream();
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            while ( is.read(buffer) != -1)
                baos.write(buffer);

            return baos.toByteArray();
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