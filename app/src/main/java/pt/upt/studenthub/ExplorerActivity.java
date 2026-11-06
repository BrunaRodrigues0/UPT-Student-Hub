package pt.upt.studenthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Ecrã principal do Módulo 3 «Explorer» (versão final da FASE 4).
 * Lista, estados, menu, Up, cancelamento e log de ciclo de vida.
 * A paisagem de dois painéis, NetworkCallback, BroadcastReceiver e atualização periódica vêm nas FASES 6–8.
 */
public class ExplorerActivity extends AppCompatActivity {

    private static final String TAG = "ExplorerActivity";

    private ExplorerViewModel viewModel;
    private TextView tvStatus, tvOffline;
    private ProgressBar progressBar;
    private Button btnRefresh, btnCancel;
    private ListView listView;
    private ItemAdapter adapter;

    // ---- Launchers (declarados como campos da classe) ----
    private final androidx.activity.result.ActivityResultLauncher<Intent> detailLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String date = result.getData().getStringExtra(DetailActivity.RESULT_DATE);
                            boolean fav  = result.getData().getBooleanExtra(DetailActivity.RESULT_FAVOURITE, false);
                            if (date != null) {
                                boolean currently = viewModel.getFavourites().getValue() != null
                                        && viewModel.getFavourites().getValue().contains(date);
                                if (currently != fav) {
                                    viewModel.toggleFavourite(date);
                                    Toast.makeText(this,
                                            fav ? R.string.favorite_added_toast
                                                    : R.string.favorite_removed_toast,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

    private final androidx.activity.result.ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
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
                                    interval
                            );
                            viewModel.setSettings(s);
                            Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show();
                            viewModel.load();
                            schedulePeriodicRefresh(s);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_explorer);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
        if (name == null) name = "";

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText(getString(R.string.explorer_greeting, name));

        tvStatus = findViewById(R.id.tvStatus);
        tvOffline = findViewById(R.id.tvOffline);
        progressBar = findViewById(R.id.progressBar);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnCancel = findViewById(R.id.btnCancel);
        listView = findViewById(R.id.listView);

        adapter = new ItemAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ExplorerViewModel.class);

        btnRefresh.setOnClickListener(v -> viewModel.load());
        btnCancel.setOnClickListener(v -> viewModel.cancel());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            DailyForecast d = adapter.getItem(position);
            if (d != null) {
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
            // recomeça a animação de entrada sempre que a lista muda
            listView.scheduleLayoutAnimation();
        });
        viewModel.getFavourites().observe(this, favs -> adapter.setFavourites(favs));

        if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.IDLE) {
            viewModel.load();
        }
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override
    protected void onPause() { Log.d(TAG, "onPause"); super.onPause(); }
    @Override
    protected void onStop() { Log.d(TAG, "onStop"); super.onStop(); }
    @Override
    protected void onDestroy() { Log.d(TAG, "onDestroy"); super.onDestroy(); }

    private void renderStatus(ExplorerViewModel.Status s) {
        boolean loading = s == ExplorerViewModel.Status.LOADING;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);
        btnCancel.setEnabled(loading);

        switch (s) {
            case IDLE:      tvStatus.setText(R.string.status_idle_explorer); break;
            case LOADING:   tvStatus.setText(R.string.status_loading_explorer); break;
            case SUCCESS: {
                int n = adapter.getCount();
                tvStatus.setText(getString(R.string.status_success_explorer, n));
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
        if (id == R.id.action_refresh) {
            viewModel.load();
            return true;
        }
        if (id == R.id.action_compare) {
            startActivity(new Intent(this, CompareActivity.class));
            return true;
        }
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
        if (id == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        if (id == R.id.action_location_porto) {
            item.setChecked(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Será ligado ao Handler.postDelayed na FASE 8 (RF14). */
    private void schedulePeriodicRefresh(ExplorerViewModel.Settings s) {
        // TODO FASE 8
    }
}