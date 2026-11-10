package pt.upt.studenthub;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.Locale;

/**
 * Detalhe de um dia (pedido B da V1).
 * Devolve o estado do favorito à ExplorerActivity via setResult (RF6).
 */
public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "pt.upt.studenthub.EXTRA_DATE";
    public static final String EXTRA_FAVOURITE = "pt.upt.studenthub.EXTRA_FAVOURITE";
    public static final String RESULT_DATE = "pt.upt.studenthub.RESULT_DATE";
    public static final String RESULT_FAVOURITE = "pt.upt.studenthub.RESULT_FAVOURITE";

    private static final String TAG = "DetailActivity";

    private ExplorerViewModel viewModel;
    private String date;
    private boolean favourite;

    private TextView tvTitle, tvStatus;
    private ProgressBar progressBar;
    private Button btnFavourite;
    private View card;
    private LinearLayout hoursContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        date = getIntent().getStringExtra(EXTRA_DATE);
        if (date == null) date = "";
        favourite = getIntent().getBooleanExtra(EXTRA_FAVOURITE, false);

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvStatus = findViewById(R.id.tvDetailStatus);
        progressBar = findViewById(R.id.detailProgress);
        btnFavourite = findViewById(R.id.btnToggleFavourite);
        card = findViewById(R.id.detailCard);
        hoursContainer = findViewById(R.id.hoursContainer);

        tvTitle.setText(getString(R.string.detail_title, date));
        updateFavouriteButton();

        viewModel = new ViewModelProvider(this).get(ExplorerViewModel.class);

        btnFavourite.setOnClickListener(v -> {
            if (favourite) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.favorite_remove_confirm_title)
                        .setMessage(R.string.favorite_remove_confirm_message)
                        .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                            favourite = false;
                            updateFavouriteButton();
                            Toast.makeText(this, R.string.favorite_removed_toast, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
            } else {
                favourite = true;
                updateFavouriteButton();
                Toast.makeText(this, R.string.favorite_added_toast, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getStatus().observe(this, this::renderStatus);
        viewModel.getError().observe(this, type -> {
            if (type != null) tvStatus.setText(errorMessage(type));
        });
        viewModel.getHourly().observe(this, this::renderHours);

        // Pedido B só se ainda não foi feito
        if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.IDLE) {
            viewModel.loadDetail(date);
        }
    }

    private void updateFavouriteButton() {
        btnFavourite.setText(favourite ? R.string.favorite_remove : R.string.favorite_add);
    }

    private void renderStatus(ExplorerViewModel.Status s) {
        boolean loading = s == ExplorerViewModel.Status.LOADING;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        switch (s) {
            case IDLE:
                tvStatus.setText(R.string.status_idle_explorer);
                break;
            case LOADING:
                tvStatus.setText(R.string.detail_status_loading);
                break;
            case SUCCESS: {
                List<HourlyForecast> h = viewModel.getHourly().getValue();
                tvStatus.setText(getString(R.string.detail_status_success,
                        h == null ? 0 : h.size()));
                break;
            }
            case EMPTY:
                tvStatus.setText(R.string.detail_status_empty);
                break;
            case CANCELLED:
                tvStatus.setText(R.string.status_cancelled_explorer);
                break;
            case ERROR:
                tvStatus.setText(R.string.detail_status_error);
                break;
        }
    }

    private String errorMessage(ExplorerViewModel.ErrorType type) {
        switch (type) {
            case TIMEOUT: return getString(R.string.err_timeout_explorer);
            case HTTP:    return getString(R.string.err_http_explorer, viewModel.getLastHttpCode().getValue());
            case PARSE:   return getString(R.string.err_parse_explorer);
            default:      return getString(R.string.err_no_connection_explorer);
        }
    }

    /** Constrói as linhas das horas dentro do cartão, com animação de entrada. */
    private void renderHours(List<HourlyForecast> hours) {
        if (hours == null || hours.isEmpty()) {
            card.setVisibility(View.GONE);
            return;
        }
        hoursContainer.removeAllViews();

        ExplorerViewModel.Settings s = viewModel.getSettings().getValue();
        int threshold = s == null ? 30 : s.rainThreshold;
        boolean fahrenheit = s != null && s.fahrenheit;
        String unit = fahrenheit ? "°F" : "°C";
        int pad = getResources().getDimensionPixelSize(R.dimen.spacing_xs);

        for (HourlyForecast h : hours) {
            TextView tv = new TextView(this);
            if (h.rainProbability >= threshold) {
                tv.setText(String.format(Locale.getDefault(), "%s · %.1f %s · %d%% rain (alert)",
                        h.hour(), h.temperature, unit, h.rainProbability));
                tv.setTextColor(getResources().getColor(R.color.upt_error, getTheme()));
            } else {
                tv.setText(String.format(Locale.getDefault(), "%s · %.1f %s", h.hour(), h.temperature, unit)
                        + " · " + getString(WeatherCodes.describe(h.weatherCode))
                        + " · " + getString(R.string.detail_hour_rain, h.rainProbability));
            }
            tv.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target));
            tv.setPadding(0, pad, 0, pad);
            hoursContainer.addView(tv);
        }

        card.setVisibility(View.VISIBLE);
        card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_enter));
    }

    @Override
    public void onBackPressed() {
        sendResultAndFinish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        sendResultAndFinish();
        return true;
    }

    /** Devolve sempre o estado do favorito à Explorer (RF6). */
    private void sendResultAndFinish() {
        Intent result = new Intent();
        result.putExtra(RESULT_DATE, date);
        result.putExtra(RESULT_FAVOURITE, favourite);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { Log.d(TAG, "onPause"); super.onPause(); }
    @Override protected void onStop() { Log.d(TAG, "onStop"); super.onStop(); }
    @Override protected void onDestroy() { Log.d(TAG, "onDestroy"); super.onDestroy(); }
}