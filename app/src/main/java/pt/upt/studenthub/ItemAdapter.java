package pt.upt.studenthub;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Adapter personalizado da ListView do Explorer, com padrão ViewHolder.
 */
public class ItemAdapter extends ArrayAdapter<DailyForecast> {

    public ItemAdapter(@NonNull Context context, @NonNull List<DailyForecast> items) {
        super(context, 0, items);
    }

    // a preencher na FASE 4
}