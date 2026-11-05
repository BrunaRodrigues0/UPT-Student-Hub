package pt.upt.studenthub;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Detalhe de um elemento (pedido B da variante V1: horas do dia escolhido).
 * Permite marcar/desmarcar favorito e devolve o resultado à ExplorerActivity.
 */
public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
    }
}
