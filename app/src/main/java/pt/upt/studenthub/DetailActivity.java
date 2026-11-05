package pt.upt.studenthub;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Detalhe de um dia (pedido B da V1: horas do dia escolhido).
 * Nesta fase só mostra a data; a UI completa e o favorito vêm na FASE 5.
 */
public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "pt.upt.studenthub.EXTRA_DATE";
    private static final String TAG = "DetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String date = getIntent().getStringExtra(EXTRA_DATE);
        if (date == null) date = "";

        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        tvTitle.setText(getString(R.string.detail_title, date));
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { Log.d(TAG, "onPause"); super.onPause(); }
    @Override protected void onStop() { Log.d(TAG, "onStop"); super.onStop(); }
    @Override protected void onDestroy() { Log.d(TAG, "onDestroy"); super.onDestroy(); }
}
