package pt.upt.studenthub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ecrã principal do Módulo 3 «Explorer».
 * Inclui lista, estados, cancelamento, menu, Up, log de ciclo de vida,
 * paisagem com dois painéis (RF10), NetworkCallback (RF13), bateria (RF15),
 * atualização periódica (RF14) e sobrevivência à rotação (RF17).
 */
public class ExplorerActivity extends AppCompatActivity {

    private static final String TAG = "ExplorerActivity";
    private static final String KEY_SELECTED_DATE = "selectedDate";
    private static final String KEY_LIST_POS = "listPos";

    private ExplorerViewModel viewModel;
    private ConnectivityManager connectivityManager;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());

    // UI
    private TextView tvStatus, tvOffline;
    private ProgressBar progressBar;
    private Button btnRefresh, btnCancel;
    private ListView listView;
    private ItemAdapter adapter;

    // Painel de detalhe (só em paisagem)
    private View detailPanel;
    private TextView tvPanelTitle, tvPanelStatus;
    private LinearLayout panelHoursContainer;
    private Button btnPanelFavourite;

    // Estado
    private String selectedDate = "";
    private boolean online = true;
    private boolean batteryLow = false;

    // ---------- Bateria ----------
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_LOW.equals(action)) {
                batteryLow = true;
                refreshHandler.removeCallbacks(refreshRunnable);
                Toast.makeText(context, R.string.toast_battery_low, Toast.LENGTH_SHORT).show();
            } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
                batteryLow = false;
                scheduleRefresh();
                Toast.makeText(context, R.string.toast_battery_okay, Toast.LENGTH_SHORT).show();
            }
        }
    };

    // ---------- Rede ----------
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(@NonNull Network network) {
                    runOnUiThread(() -> setOnline(true));
                }
                @Override public void onLost(@NonNull Network network) {
                    runOnUiThread(() -> setOnline(false));
                }
            };

    // ---------- Refresh periódico ----------
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            viewModel.load();
            scheduleRefresh();
        }
    };

    // ---------- Launchers ----------
    private final ActivityResultLauncher<Intent> detailLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String date = result.getData().getStringExtra(DetailActivity.RESULT_DATE);
                            boolean fav  = result.getData().getBooleanExtra(DetailActivity.RESULT_FAVOURITE, false);
                            if (date != null) {
                                boolean current = viewModel.getFavourites().getValue() != null
                                        && viewModel.getFavourites().getValue().contains(date);
                                if (current != fav) {
                                    viewModel.toggleFavourite(date);
                                    Toast.makeText(this,
                                            fav ? R.string.favorite_added_toast
                                                    : R.string.favorite_removed_toast,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent d = result.getData();
                            ExplorerViewModel.Settings current = viewModel.getSettings().getValue();
                            int interval = d.getIntExtra(SettingsActivity.EXTRA_REFRESH_SEC,
                                    current == null ? 300 : current.refreshIntervalSec);
                            ExplorerViewModel.Settings s = new ExplorerViewModel.Settings(
                                    d.getBooleanExtra(SettingsActivity.EXTRA_FAHRENHEIT, false),
                                    d.getIntExtra(SettingsActivity.EXTRA_DAYS, 7),
                                    d.getIntExtra(SettingsActivity.EXTRA_RAIN, 30),
                                    d.getBooleanExtra(SettingsActivity.EXTRA_AUTO_REFRESH, true),
                                    interval);
                            viewModel.setSettings(s);
                            Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show();
                            viewModel.load();
                            scheduleRefresh();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_explorer);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewModel = new ViewModelProvider(this).get(ExplorerViewModel.class);

        String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
        if (name != null && !name.trim().isEmpty()) {
            viewModel.setStudentName(name);
        }

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        viewModel.getStudentName().observe(this, n -> {
            String studentName = (n == null) ? "" : n;
            tvGreeting.setText(getString(R.string.explorer_greeting, studentName));
        });

        tvStatus = findViewById(R.id.tvStatus);
        tvOffline = findViewById(R.id.tvOffline);
        progressBar = findViewById(R.id.progressBar);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnCancel = findViewById(R.id.btnCancel);
        listView = findViewById(R.id.listView);

        // Painel de detalhe: só existe em paisagem
        detailPanel = findViewById(R.id.detailPanel);
        if (detailPanel != null) {
            tvPanelTitle = findViewById(R.id.tvPanelTitle);
            tvPanelStatus = findViewById(R.id.tvPanelStatus);
            panelHoursContainer = findViewById(R.id.panelHoursContainer);
            btnPanelFavourite = findViewById(R.id.btnPanelFavourite);
            btnPanelFavourite.setOnClickListener(v -> {
                if (!selectedDate.isEmpty()) {
                    viewModel.toggleFavourite(selectedDate);
                    updatePanelFavourite();
                }
            });
        }

        if (savedInstanceState != null) {
            selectedDate = savedInstanceState.getString(KEY_SELECTED_DATE, "");
        }

        adapter = new ItemAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);

        viewModel.getSettings().observe(this, s -> {
            if (s != null) {
                adapter.setFahrenheit(s.fahrenheit);
            }
        });
        connectivityManager = ContextCompat.getSystemService(this, ConnectivityManager.class);

        btnRefresh.setOnClickListener(v -> viewModel.load());
        btnCancel.setOnClickListener(v -> viewModel.cancel());

        // Clique: paisagem => painel; retrato => DetailActivity
        listView.setOnItemClickListener((parent, view, position, id) -> {
            DailyForecast d = adapter.getItem(position);
            if (d == null) return;
            if (detailPanel != null) {
                selectedDate = d.date;
                tvPanelTitle.setText(getString(R.string.detail_title, selectedDate));
                updatePanelFavourite();
                viewModel.loadDetail(selectedDate);
            } else {
                Intent i = new Intent(this, DetailActivity.class);
                i.putExtra(DetailActivity.EXTRA_DATE, d.date);
                boolean isFav = viewModel.getFavourites().getValue() != null
                        && viewModel.getFavourites().getValue().contains(d.date);
                i.putExtra(DetailActivity.EXTRA_FAVOURITE, isFav);
                detailLauncher.launch(i);
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            DailyForecast d = adapter.getItem(position);
            if (d == null) return false;
            boolean isFav = viewModel.getFavourites().getValue() != null
                    && viewModel.getFavourites().getValue().contains(d.date);
            if (!isFav) return false;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.favorite_remove_confirm_title)
                    .setMessage(R.string.favorite_remove_confirm_message)
                    .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                        viewModel.toggleFavourite(d.date);
                        Toast.makeText(this, R.string.favorite_removed_toast, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            return true;
        });

        viewModel.getStatus().observe(this, this::renderStatus);
        viewModel.getError().observe(this, type -> {
            if (type != null) tvStatus.setText(errorMessage(type));
        });
        viewModel.getDaily().observe(this, list -> {
            adapter.clear();
            if (list != null) adapter.addAll(list);
            adapter.notifyDataSetChanged();
            listView.scheduleLayoutAnimation();
            if (savedInstanceState == null) return;
            int pos = savedInstanceState.getInt(KEY_LIST_POS, 0);
            listView.setSelection(pos);
        });
        viewModel.getFavourites().observe(this, favs -> {
            adapter.setFavourites(favs);
            updatePanelFavourite();
        });
        viewModel.getHourly().observe(this, this::renderPanelHours);

        if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.IDLE) {
            viewModel.load();
        }
    }

    // ---------- Ciclo de vida + registos ----------
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // Bateria
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_LOW);
        batteryFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        ContextCompat.registerReceiver(this, batteryReceiver, batteryFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        // Rede
        setOnline(isOnlineNow());
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        // Refresh periódico
        scheduleRefresh();
    }

    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override
    protected void onPause() { Log.d(TAG, "onPause"); super.onPause(); }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        try { unregisterReceiver(batteryReceiver); } catch (IllegalArgumentException ignored) { }
        connectivityManager.unregisterNetworkCallback(networkCallback);
        refreshHandler.removeCallbacks(refreshRunnable);
        super.onStop();
    }

    @Override
    protected void onDestroy() { Log.d(TAG, "onDestroy"); super.onDestroy(); }

    // ---------- Estado online/offline ----------
    private boolean isOnlineNow() {
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void setOnline(boolean nowOnline) {
        boolean cameBack = !online && nowOnline;
        online = nowOnline;
        tvOffline.setVisibility(online ? View.GONE : View.VISIBLE);
        if (cameBack
                && viewModel.getStatus().getValue() == ExplorerViewModel.Status.ERROR) {
            Toast.makeText(this, R.string.toast_back_online_explorer, Toast.LENGTH_SHORT).show();
            viewModel.load();
        }
    }

    // ---------- Refresh periódico ----------
    private void scheduleRefresh() {
        refreshHandler.removeCallbacks(refreshRunnable);
        ExplorerViewModel.Settings s = viewModel.getSettings().getValue();
        if (s == null || !s.autoRefresh || batteryLow) return;
        long intervalSec = s.refreshIntervalSec > 0 ? s.refreshIntervalSec : 300L;
        refreshHandler.postDelayed(refreshRunnable, intervalSec * 1000L);
    }

    // ---------- Painel (paisagem) ----------
    private void updatePanelFavourite() {
        if (btnPanelFavourite == null) return;
        boolean isFav = selectedDate != null && !selectedDate.isEmpty()
                && viewModel.getFavourites().getValue() != null
                && viewModel.getFavourites().getValue().contains(selectedDate);
        btnPanelFavourite.setText(isFav ? R.string.favorite_remove : R.string.favorite_add);
        btnPanelFavourite.setVisibility(selectedDate == null || selectedDate.isEmpty()
                ? View.GONE : View.VISIBLE);
    }

    private void renderPanelHours(List<HourlyForecast> hours) {
        if (panelHoursContainer == null) return;
        panelHoursContainer.removeAllViews();
        if (hours == null || hours.isEmpty()) {
            if (tvPanelStatus != null) tvPanelStatus.setText(R.string.detail_status_empty);
            return;
        }
        if (tvPanelStatus != null) {
            tvPanelStatus.setText(getString(R.string.detail_status_success, hours.size()));
        }
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
                tv.setTextColor(ContextCompat.getColor(this, R.color.upt_error));
            } else {
                tv.setText(String.format(Locale.getDefault(), "%s · %.1f %s", h.hour(), h.temperature, unit)
                        + " · " + getString(WeatherCodes.describe(h.weatherCode))
                        + " · " + getString(R.string.detail_hour_rain, h.rainProbability));
            }
            tv.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target));
            tv.setPadding(0, pad, 0, pad);
            panelHoursContainer.addView(tv);
        }
    }

    // ---------- Estados ----------
    private void renderStatus(ExplorerViewModel.Status s) {
        boolean loading = s == ExplorerViewModel.Status.LOADING;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);
        btnCancel.setEnabled(loading);
        switch (s) {
            case IDLE:      tvStatus.setText(R.string.status_idle_explorer); break;
            case LOADING:   tvStatus.setText(R.string.status_loading_explorer); break;
            case SUCCESS: {
                List<DailyForecast> list = viewModel.getDaily().getValue();
                int count = (list != null && !list.isEmpty()) ? list.size() : adapter.getCount();
                tvStatus.setText(getString(R.string.status_success_explorer, count));
                break;
            }
            case EMPTY:     tvStatus.setText(R.string.status_empty_explorer); break;
            case CANCELLED: tvStatus.setText(R.string.status_cancelled_explorer); break;
            case ERROR:     tvStatus.setText(R.string.status_error_explorer); break;
        }
        invalidateOptionsMenu();
    }

    private String errorMessage(ExplorerViewModel.ErrorType type) {
        switch (type) {
            case TIMEOUT: return getString(R.string.err_timeout_explorer);
            case HTTP:    return getString(R.string.err_http_explorer, viewModel.getLastHttpCode().getValue());
            case PARSE:   return getString(R.string.err_parse_explorer);
            default:      return getString(R.string.err_no_connection_explorer);
        }
    }

    // ---------- Rotação ----------
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_DATE, selectedDate);
        int pos = listView != null ? listView.getFirstVisiblePosition() : 0;
        outState.putInt(KEY_LIST_POS, pos);
    }

    // ---------- Menu ----------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_explorer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean loading = viewModel.getStatus().getValue() == ExplorerViewModel.Status.LOADING;
        menu.findItem(R.id.action_refresh).setEnabled(!loading);
        menu.findItem(R.id.action_compare).setEnabled(!loading);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) { viewModel.load(); return true; }
        if (id == R.id.action_compare) { startActivity(new Intent(this, CompareActivity.class)); return true; }
        if (id == R.id.action_settings) {
            ExplorerViewModel.Settings s = viewModel.getSettings().getValue();
            Intent i = new Intent(this, SettingsActivity.class);
            if (s != null) {
                i.putExtra(SettingsActivity.EXTRA_FAHRENHEIT, s.fahrenheit);
                i.putExtra(SettingsActivity.EXTRA_DAYS, s.days);
                i.putExtra(SettingsActivity.EXTRA_RAIN, s.rainThreshold);
                i.putExtra(SettingsActivity.EXTRA_AUTO_REFRESH, s.autoRefresh);
                i.putExtra(SettingsActivity.EXTRA_REFRESH_SEC, s.refreshIntervalSec);
            }
            settingsLauncher.launch(i);
            return true;
        }
        if (id == R.id.action_about) { AboutDialog.show(this); return true; }
        if (id == R.id.action_location_porto) { item.setChecked(true); return true; }
        return super.onOptionsItemSelected(item);
    }
}