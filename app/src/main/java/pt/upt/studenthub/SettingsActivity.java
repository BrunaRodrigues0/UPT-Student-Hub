package pt.upt.studenthub;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Definições da variante V1: unidade de temperatura, número de dias e limiar de chuva.
 * Valida os valores e devolve as escolhas à ExplorerActivity como resultado.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
}
