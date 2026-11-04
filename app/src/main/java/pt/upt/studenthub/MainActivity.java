package pt.upt.studenthub;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Ecrã inicial. Dois pontos de entrada: Timetable (Módulo 1) e Campus weather (Módulo 2).
 * Ambos reutilizam a mesma validação do nome.
 */
public class MainActivity extends AppCompatActivity {

    private EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableStrictModeInDebug();
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        Button btnOpenTimetable = findViewById(R.id.btnOpenTimetable);
        Button btnOpenWeather = findViewById(R.id.btnOpenWeather);

        btnOpenTimetable.setOnClickListener(v -> openWith(TimetableActivity.class));
        btnOpenWeather.setOnClickListener(v -> openWith(WeatherActivity.class));
    }

    /** Valida o nome (Week 3) e abre o ecrã alvo com o nome como extra (Week 4). */
    private void openWith(Class<?> target) {
        String name = readValidName();
        if (name == null) return;
        Intent intent = new Intent(this, target);
        intent.putExtra(TimetableActivity.EXTRA_STUDENT_NAME, name);
        startActivity(intent);
    }

    @Nullable
    private String readValidName() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError(getString(R.string.error_name_required));
            return null;
        }
        return name;
    }

    /**
     * StrictMode em debug: apanha más práticas.
     * penaltyDeathOnNetwork() mata o processo se houver rede na main thread.
     */
    private void enableStrictModeInDebug() {
        boolean debuggable =
                (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (debuggable) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .penaltyDeathOnNetwork()
                    .build());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}