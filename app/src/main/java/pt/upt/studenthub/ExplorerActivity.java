package pt.upt.studenthub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/**
 * Ecrã principal do Módulo 3 «Explorer» (versão mínima da FASE 3).
 * Lista, estados e botão Cancelar. O menu e a paisagem vêm nas fases seguintes.
 */
public class ExplorerActivity extends AppCompatActivity {

    private ExplorerViewModel viewModel;

    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnRefresh;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
        if (name == null) name = "";

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText(getString(R.string.explorer_greeting, name));

        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnCancel = findViewById(R.id.btnCancel);

        viewModel = new ViewModelProvider(this).get(ExplorerViewModel.class);

        btnRefresh.setOnClickListener(v -> viewModel.load());
        btnCancel.setOnClickListener(v -> viewModel.cancel());

        viewModel.getStatus().observe(this, this::renderStatus);
        viewModel.getError().observe(this, type -> {
            if (type != null) tvStatus.setText(errorMessage(type));
        });
        viewModel.getLastHttpCode().observe(this, code -> {
            // nada a fazer aqui, o erro já mostra o texto
        });
        viewModel.getDaily().observe(this, list -> {
            // na FASE 4 isto liga ao ItemAdapter
        });

        // Primeiro carregamento automático (RF3), só se ainda não foi feito
        if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.IDLE) {
            viewModel.load();
        }
    }

    private void renderStatus(ExplorerViewModel.Status s) {
        boolean loading = s == ExplorerViewModel.Status.LOADING;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);
        btnCancel.setEnabled(loading);

        switch (s) {
            case IDLE:
                tvStatus.setText(R.string.status_idle_explorer);
                break;
            case LOADING:
                tvStatus.setText(R.string.status_loading_explorer);
                break;
            case SUCCESS:
                tvStatus.setText(getString(R.string.status_success_explorer,
                        viewModel.getDaily().getValue() == null ? 0 : viewModel.getDaily().getValue().size()));
                break;
            case EMPTY:
                tvStatus.setText(R.string.status_empty_explorer);
                break;
            case CANCELLED:
                tvStatus.setText(R.string.status_cancelled_explorer);
                break;
            case ERROR:
                tvStatus.setText(R.string.status_error_explorer);
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
}
