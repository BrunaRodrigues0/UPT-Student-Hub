package pt.upt.studenthub;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/**
 * Mostra a caixa de diálogo "Sobre". Usada por ambas as atividades (evita duplicação).
 */
public final class AboutDialog {

    private AboutDialog() { }

    /** Mostra a caixa de diálogo "Sobre" no contexto fornecido. */
    public static void show(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
