package pt.upt.studenthub;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Comparação de três locais (Porto, Lisboa, Faro) com três pedidos em paralelo.
 * Falha parcial por local: os que respondem aparecem; o que falha mostra o erro.
 */
public class CompareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);
    }
}
