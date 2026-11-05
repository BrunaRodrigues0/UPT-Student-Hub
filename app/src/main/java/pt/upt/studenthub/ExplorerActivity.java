package pt.upt.studenthub;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Ecrã principal do Módulo 3 «Explorer».
 * Lista (pedido A), menu, estados, faixa offline, recetor de bateria e atualização periódica.
 * Em paisagem usa um layout com lista + painel de detalhe lado a lado (sem Fragments).
 */
public class ExplorerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
    }
}
