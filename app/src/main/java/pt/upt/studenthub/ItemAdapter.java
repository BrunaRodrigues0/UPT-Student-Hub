package pt.upt.studenthub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter da ListView do Explorer, com padrão ViewHolder.
 * Mostra data, descrição WMO, máx/mín e prob. de chuva + ícone que muda com o código WMO (RF2).
 */
public class ItemAdapter extends ArrayAdapter<DailyForecast> {

    /** ViewHolder: guarda as referências para evitar findViewById a cada getView. */
    private static final class ViewHolder {
        ImageView icon;
        ImageView favourite;
        TextView date;
        TextView description;
        TextView temps;
        TextView rain;
    }

    private final Context context;
    private final List<DailyForecast> items;
    private Set<String> favourites = new HashSet<>();
    private boolean fahrenheit = false;

    public ItemAdapter(@NonNull Context context, @NonNull List<DailyForecast> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    /** A Explorer passa aqui o conjunto de favoritos para mostrar/esconder o ícone. */
    public void setFavourites(@Nullable Set<String> favourites) {
        this.favourites = favourites == null ? new HashSet<>() : favourites;
        notifyDataSetChanged();
    }

    public void setFahrenheit(boolean fahrenheit) {
        this.fahrenheit = fahrenheit;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_daily, parent, false);
            h = new ViewHolder();
            h.icon = convertView.findViewById(R.id.ivIcon);
            h.favourite = convertView.findViewById(R.id.ivFavourite);
            h.date = convertView.findViewById(R.id.tvDate);
            h.description = convertView.findViewById(R.id.tvDescription);
            h.temps = convertView.findViewById(R.id.tvTemps);
            h.rain = convertView.findViewById(R.id.tvRain);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        DailyForecast d = items.get(position);

        String unit = fahrenheit ? "°F" : "°C";
        h.date.setText(formatDate(d.date));
        h.description.setText(WeatherCodes.describe(d.weatherCode));
        h.temps.setText(String.format(Locale.getDefault(), "Max: %.1f %s · Min: %.1f %s", d.tempMax, unit, d.tempMin, unit));
        h.rain.setText(context.getString(R.string.day_rain_prob, d.rainProbability));

        h.icon.setImageResource(iconForCode(d.weatherCode));

        if (favourites.contains(d.date)) {
            h.favourite.setVisibility(View.VISIBLE);
            h.favourite.setImageResource(R.drawable.ic_favorite_on);
        } else {
            h.favourite.setVisibility(View.GONE);
        }

        return convertView;
    }

    /** "2026-11-03" → "Tue 03/11" (dia da semana + dia/mês). */
    private String formatDate(String iso) {
        // Simples e sem dependências: mostra ISO ou uma versão curta legível
        if (iso == null || iso.length() < 10) return iso == null ? "" : iso;
        try {
            LocalDate d = LocalDate.parse(iso);
            return d.getDayOfWeek().toString().substring(0, 3)
                    + " " + String.format(Locale.getDefault(),
                    "%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
        } catch (Exception e) {
            return iso;
        }
    }

    /** WMO → ícone vetorial (4 variantes simplificadas para o RF2). */
    private int iconForCode(int code) {
        if (code == 0 || code == 1) return R.drawable.ic_weather_clear;
        if (code >= 2 && code <= 3) return R.drawable.ic_weather_cloudy;
        if (code >= 45 && code <= 67) return R.drawable.ic_weather_rain;
        if (code >= 71 && code <= 77) return R.drawable.ic_weather_rain;
        if (code >= 80 && code <= 82) return R.drawable.ic_weather_rain;
        if (code == 85 || code == 86) return R.drawable.ic_weather_rain;
        if (code >= 95) return R.drawable.ic_weather_storm;
        return R.drawable.ic_weather_cloudy;
    }
}