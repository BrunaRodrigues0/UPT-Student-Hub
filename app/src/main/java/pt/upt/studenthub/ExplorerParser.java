package pt.upt.studenthub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converte o JSON do Open-Meteo em modelos imutáveis (org.json).
 * Trata valores nulos com isNull/opt* (RT5) e arrays paralelos por índice.
 */
public final class ExplorerParser {

    private ExplorerParser() { }

    /**
     * Pedido A da V1: {"daily": {"time":[...], "weather_code":[...],
     * "temperature_2m_max":[...], "temperature_2m_min":[...],
     * "precipitation_probability_max":[...]}}
     */
    public static List<DailyForecast> parseDaily(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject daily = root.getJSONObject("daily");

        JSONArray times  = daily.getJSONArray("time");
        JSONArray codes  = daily.optJSONArray("weather_code");
        JSONArray tMax   = daily.optJSONArray("temperature_2m_max");
        JSONArray tMin   = daily.optJSONArray("temperature_2m_min");
        JSONArray rain   = daily.optJSONArray("precipitation_probability_max");

        int n = times.length();
        if (n == 0) return Collections.emptyList();

        List<DailyForecast> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new DailyForecast(
                    times.getString(i),
                    optInt(codes, i),
                    optDouble(tMax, i),
                    optDouble(tMin, i),
                    optInt(rain, i)
            ));
        }
        return out;
    }

    /**
     * Pedido B da V1: {"hourly": {"time":[...], "temperature_2m":[...],
     * "precipitation_probability":[...], "weather_code":[...]}}
     */
    public static List<HourlyForecast> parseHourly(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject hourly = root.getJSONObject("hourly");

        JSONArray times = hourly.getJSONArray("time");
        JSONArray temps = hourly.optJSONArray("temperature_2m");
        JSONArray rain  = hourly.optJSONArray("precipitation_probability");
        JSONArray codes = hourly.optJSONArray("weather_code");

        int n = times.length();
        if (n == 0) return Collections.emptyList();

        List<HourlyForecast> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new HourlyForecast(
                    times.getString(i),
                    optDouble(temps, i),
                    optInt(rain, i),
                    optInt(codes, i)
            ));
        }
        return out;
    }

    // ---- Helpers: arrays paralelos podem ser null ou ter valores null ----

    private static int optInt(JSONArray a, int i) {
        if (a == null || i >= a.length() || a.isNull(i)) return 0;
        return a.optInt(i, 0);
    }

    private static double optDouble(JSONArray a, int i) {
        if (a == null || i >= a.length() || a.isNull(i)) return 0.0;
        return a.optDouble(i, 0.0);
    }
}
