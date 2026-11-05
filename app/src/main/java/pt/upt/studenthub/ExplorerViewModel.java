package pt.upt.studenthub;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel do Módulo 3: dono do trabalho de rede e do estado da UI.
 * REGRA: sem Context/Activity/View aqui (RT7).
 */
public class ExplorerViewModel extends ViewModel {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // a preencher na FASE 3
}
