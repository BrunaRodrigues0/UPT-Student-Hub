package pt.upt.studenthub;

import android.net.Uri;

/**
 * Constrói os URLs do Open-Meteo (HTTPS, sem chave de API).
 * Uri.Builder faz a codificação correta de cada parte.
 */
public final class WeatherApi {

    public static final double UPT_LATITUDE = 41.18;
    public static final double UPT_LONGITUDE = -8.61;

    private WeatherApi() { }

    /** Pedido normal ao endpoint forecast. */
    public static String currentWeatherUrl() {
        return build("forecast");
    }

    /** Mesmo pedido para um caminho que não existe (404). */
    public static String brokenUrl() {
        return build("forecast-does-not-exist");
    }

    private static String build(String path) {
        return new Uri.Builder()
                .scheme("https")
                .authority("api.open-meteo.com")
                .appendPath("v1")
                .appendPath(path)
                .appendQueryParameter("latitude", String.valueOf(UPT_LATITUDE))
                .appendQueryParameter("longitude", String.valueOf(UPT_LONGITUDE))
                .appendQueryParameter("current",
                        "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code")
                .appendQueryParameter("timezone", "Europe/Lisbon")
                .build()
                .toString();
    }
}
