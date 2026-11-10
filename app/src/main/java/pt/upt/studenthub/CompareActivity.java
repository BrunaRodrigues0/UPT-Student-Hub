package pt.upt.studenthub;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

/**
 * Comparação de Porto, Lisboa e Faro com três pedidos em paralelo (RF9).
 * A ViewModel entrega os três resultados; os que respondem aparecem, o que falha mostra o erro.
 */
public class CompareActivity extends AppCompatActivity {

    private ExplorerViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(ExplorerViewModel.class);

        // O callback da ViewModel corre na main thread (mainHandler.post lá dentro)
        viewModel.compare(this::renderResults);
    }

    private void renderResults(@NonNull ExplorerViewModel.CompareResult[] results) {
        int ok = 0;
        for (ExplorerViewModel.CompareResult r : results) {
            if (r.data != null) ok++;
        }
        TextView status = findViewById(R.id.tvCompareStatus);
        status.setText(getString(R.string.compare_success, ok));

        renderCard(
                results[0],
                findViewById(R.id.tvPortoMax),
                findViewById(R.id.tvPortoMin),
                findViewById(R.id.tvPortoRain));

        renderCard(
                results[1],
                findViewById(R.id.tvLisboaMax),
                findViewById(R.id.tvLisboaMin),
                findViewById(R.id.tvLisboaRain));

        renderCard(
                results[2],
                findViewById(R.id.tvFaroMax),
                findViewById(R.id.tvFaroMin),
                findViewById(R.id.tvFaroRain));
    }

    private void renderCard(ExplorerViewModel.CompareResult r,
                            TextView tvMax, TextView tvMin, TextView tvRain) {
        if (r == null) {
            tvMax.setText(R.string.compare_loading);
            tvMin.setText("");
            tvRain.setText("");
            return;
        }
        if (r.data == null || r.data.isEmpty()) {
            String msg = errorFor(r);
            tvMax.setText(msg);
            tvMin.setText("");
            tvRain.setText("");
            return;
        }
        ExplorerViewModel.Settings s = viewModel.getSettings().getValue();
        boolean fahrenheit = s != null && s.fahrenheit;
        String unit = fahrenheit ? "°F" : "°C";

        // A V1 pede "hoje" — o primeiro item da lista diária (forecast_days=1) é hoje
        DailyForecast today = r.data.get(0);
        tvMax.setText(String.format(Locale.getDefault(), "Max: %.1f %s", today.tempMax, unit));
        tvMin.setText(String.format(Locale.getDefault(), "Min: %.1f %s", today.tempMin, unit));
        tvRain.setText(getString(R.string.compare_rain, today.rainProbability));
    }

    private String errorFor(ExplorerViewModel.CompareResult r) {
        String text;
        switch (r.errorType) {
            case TIMEOUT: text = getString(R.string.err_timeout_explorer); break;
            case HTTP:    text = getString(R.string.err_http_explorer, r.httpCode); break;
            case PARSE:   text = getString(R.string.err_parse_explorer); break;
            default:      text = getString(R.string.err_no_connection_explorer); break;
        }
        return getString(R.string.compare_error, text);
    }
}
