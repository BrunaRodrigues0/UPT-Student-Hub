package pt.upt.studenthub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Definições da variante V1.
 * Valida e devolve as escolhas à ExplorerActivity via setResult (RF8).
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String EXTRA_FAHRENHEIT = "pt.upt.studenthub.SET_FAHRENHEIT";
    public static final String EXTRA_DAYS = "pt.upt.studenthub.SET_DAYS";
    public static final String EXTRA_RAIN = "pt.upt.studenthub.SET_RAIN";
    public static final String EXTRA_AUTO_REFRESH = "pt.upt.studenthub.SET_AUTO_REFRESH";
    public static final String EXTRA_REFRESH_SEC = "pt.upt.studenthub.SET_REFRESH_SEC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RadioGroup rgUnit = findViewById(R.id.rgUnit);
        RadioButton rbCelsius = findViewById(R.id.rbCelsius);
        RadioButton rbFahrenheit = findViewById(R.id.rbFahrenheit);

        RadioGroup rgDays = findViewById(R.id.rgDays);
        RadioButton rbDays3 = findViewById(R.id.rbDays3);
        RadioButton rbDays5 = findViewById(R.id.rbDays5);
        RadioButton rbDays7 = findViewById(R.id.rbDays7);

        EditText etRain = findViewById(R.id.etRainThreshold);
        CheckBox cbAuto = findViewById(R.id.cbAutoRefresh);
        EditText etInterval = findViewById(R.id.etRefreshInterval);
        Button btnSave = findViewById(R.id.btnSave);

        // Preenche a partir do extra vindo da Explorer
        boolean fahrenheit = getIntent().getBooleanExtra(EXTRA_FAHRENHEIT, false);
        int days = getIntent().getIntExtra(EXTRA_DAYS, 7);
        int rain = getIntent().getIntExtra(EXTRA_RAIN, 30);
        boolean autoRefresh = getIntent().getBooleanExtra(EXTRA_AUTO_REFRESH, true);
        int interval = getIntent().getIntExtra(EXTRA_REFRESH_SEC, 300);

        (fahrenheit ? rbFahrenheit : rbCelsius).setChecked(true);
        if (days == 3) rbDays3.setChecked(true);
        else if (days == 5) rbDays5.setChecked(true);
        else rbDays7.setChecked(true);

        etRain.setText(String.valueOf(rain));
        cbAuto.setChecked(autoRefresh);
        etInterval.setText(String.valueOf(interval));

        btnSave.setOnClickListener(v -> {
            int rainVal = parseOrMinus(etRain.getText().toString(), -1);
            if (rainVal < 0 || rainVal > 100) {
                etRain.setError(getString(R.string.settings_error_threshold));
                return;
            }
            int intervalVal = parseOrMinus(etInterval.getText().toString(), -1);
            if (intervalVal < 60 || intervalVal > 86400) {
                etInterval.setError("Enter a number between 60 and 86400");
                return;
            }

            int daysVal = rgDays.getCheckedRadioButtonId() == R.id.rbDays3 ? 3
                    : (rgDays.getCheckedRadioButtonId() == R.id.rbDays5 ? 5 : 7);
            boolean fVal = rgUnit.getCheckedRadioButtonId() == R.id.rbFahrenheit;

            Intent result = new Intent();
            result.putExtra(EXTRA_FAHRENHEIT, fVal);
            result.putExtra(EXTRA_DAYS, daysVal);
            result.putExtra(EXTRA_RAIN, rainVal);
            result.putExtra(EXTRA_AUTO_REFRESH, cbAuto.isChecked());
            result.putExtra(EXTRA_REFRESH_SEC, intervalVal);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private static int parseOrMinus(String s, int fallback) {
        if (s == null || TextUtils.isEmpty(s)) return fallback;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
