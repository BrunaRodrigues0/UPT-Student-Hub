package pt.upt.studenthub;

import android.net.Uri;

/**
 * Constrói todos os URLs do Módulo 3 (variante V1) com Uri.Builder.
 * REGRA: nenhum URL é construído fora desta classe (RT4).
 *
 * Os overloads com {@code fahrenheit} acrescentam o parâmetro
 * {@code temperature_unit=fahrenheit} ao pedido, para que o servidor
 * devolva as temperaturas na unidade escolhida nas Definições (FASE 6).
 */
public final class ExplorerApi {

    // Porto — UPT campus (V1, lista e detalhe)
    public static final double PORTO_LAT = 41.15;
    public static final double PORTO_LON = -8.61;

    // Comparação V1
    public static final double LISBOA_LAT = 38.72;
    public static final double LISBOA_LON = -9.14;
    public static final double FARO_LAT   = 37.02;
    public static final double FARO_LON   = -7.93;

    private ExplorerApi() { }

    /** Pedido A da V1: previsão diária em Celsius. */
    public static String dailyUrl(double lat, double lon, int days) {
        return dailyUrl(lat, lon, days, false);
    }

    /** Pedido A da V1: previsão diária, com unidade configurável. */
    public static String dailyUrl(double lat, double lon, int days, boolean fahrenheit) {
        Uri.Builder b = base()
                .appendQueryParameter("latitude", String.valueOf(lat))
                .appendQueryParameter("longitude", String.valueOf(lon))
                .appendQueryParameter("daily",
                        "weather_code,temperature_2m_max,temperature_2m_min,"
                                + "precipitation_probability_max")
                .appendQueryParameter("forecast_days", String.valueOf(days))
                .appendQueryParameter("timezone", "Europe/Lisbon");
        if (fahrenheit) {
            b.appendQueryParameter("temperature_unit", "fahrenheit");
        }
        return b.build().toString();
    }

    /** Pedido B da V1: horas de um dia, em Celsius. start_date/end_date = "YYYY-MM-DD". */
    public static String hourlyUrl(double lat, double lon, String isoDate) {
        return hourlyUrl(lat, lon, isoDate, false);
    }

    /** Pedido B da V1: horas de um dia, com unidade configurável. */
    public static String hourlyUrl(double lat, double lon, String isoDate, boolean fahrenheit) {
        Uri.Builder b = base()
                .appendQueryParameter("latitude", String.valueOf(lat))
                .appendQueryParameter("longitude", String.valueOf(lon))
                .appendQueryParameter("hourly",
                        "temperature_2m,precipitation_probability,weather_code")
                .appendQueryParameter("start_date", isoDate)
                .appendQueryParameter("end_date", isoDate)
                .appendQueryParameter("timezone", "Europe/Lisbon");
        if (fahrenheit) {
            b.appendQueryParameter("temperature_unit", "fahrenheit");
        }
        return b.build().toString();
    }

    private static Uri.Builder base() {
        return new Uri.Builder()
                .scheme("https")
                .authority("api.open-meteo.com")
                .appendPath("v1")
                .appendPath("forecast");
    }
}
