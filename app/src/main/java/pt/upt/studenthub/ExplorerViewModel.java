package pt.upt.studenthub;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ViewModel do Módulo 3 (variante V1).
 * Dono do trabalho de rede e do estado da UI.
 * REGRA: sem Context/Activity/View aqui (RT7).
 */
public class ExplorerViewModel extends ViewModel {

    public enum Status { IDLE, LOADING, SUCCESS, EMPTY, ERROR, CANCELLED }
    public enum ErrorType { NO_CONNECTION, TIMEOUT, HTTP, PARSE }

    // --- Definições da V1 (valores por defeito) ---
    public static final class Settings {
        public final boolean fahrenheit;
        public final int days;               // 3, 5 ou 7
        public final int rainThreshold;      // 0..100
        public final boolean autoRefresh;    // liga/desliga a atualização periódica (RF14)
        public final int refreshIntervalSec; // segundos

        public Settings(boolean fahrenheit, int days, int rainThreshold,
                        boolean autoRefresh, int refreshIntervalSec) {
            this.fahrenheit = fahrenheit;
            this.days = days;
            this.rainThreshold = rainThreshold;
            this.autoRefresh = autoRefresh;
            this.refreshIntervalSec = refreshIntervalSec;
        }
    }

    // Executor para a lista/detalhe (uma thread de cada vez) e pool para a comparação (3 em paralelo)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService comparePool = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Estado da UI
    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private final MutableLiveData<ErrorType> error = new MutableLiveData<>();
    private final MutableLiveData<Integer> lastHttpCode = new MutableLiveData<>(0);

    // Dados da V1
    private final MutableLiveData<List<DailyForecast>> daily = new MutableLiveData<>();
    private final MutableLiveData<List<HourlyForecast>> hourly = new MutableLiveData<>();

    // Favoritos e definições (em memória — persistem só até fechar a app, como o enunciado pede)
    private static final MutableLiveData<Set<String>> favourites = new MutableLiveData<>(Collections.emptySet());
    private static final MutableLiveData<Settings> settings = new MutableLiveData<>(
            new Settings(false, 7, 30, true, 300));

    private Future<?> currentTask;

    // --- Exposição read-only ---
    public LiveData<Status> getStatus() { return status; }
    public LiveData<ErrorType> getError() { return error; }
    public LiveData<Integer> getLastHttpCode() { return lastHttpCode; }
    public LiveData<List<DailyForecast>> getDaily() { return daily; }
    public LiveData<List<HourlyForecast>> getHourly() { return hourly; }
    public LiveData<Set<String>> getFavourites() { return favourites; }
    public LiveData<Settings> getSettings() { return settings; }

    private static final MutableLiveData<String> studentName = new MutableLiveData<>("");
    public LiveData<String> getStudentName() { return studentName; }
    public void setStudentName(String name) { studentName.setValue(name); }

    public static final class LocationInfo {
        public final String name;
        public final double lat;
        public final double lon;

        public LocationInfo(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    private static final MutableLiveData<LocationInfo> selectedLocation = new MutableLiveData<>(
            new LocationInfo("Porto (UPT campus)", ExplorerApi.PORTO_LAT, ExplorerApi.PORTO_LON));

    public LiveData<LocationInfo> getSelectedLocation() { return selectedLocation; }
    public void setSelectedLocation(LocationInfo loc) { selectedLocation.setValue(loc); }

    // ---------- LISTA (pedido A) ----------
    /** Chamado na UI thread. Carrega os dias com as definições atuais. */
    public void load() {
        if (status.getValue() == Status.LOADING) return;

        Settings s = settings.getValue();
        if (s == null) s = new Settings(false, 7, 30, true, 300);

        LocationInfo loc = selectedLocation.getValue();
        if (loc == null) loc = new LocationInfo("Porto (UPT campus)", ExplorerApi.PORTO_LAT, ExplorerApi.PORTO_LON);

        final int days = s.days;
        final String url = ExplorerApi.dailyUrl(
                loc.lat, loc.lon, days, s.fahrenheit);

        status.setValue(Status.LOADING);
        currentTask = executor.submit(() -> {
            try {
                String body = HttpClient.get(url);
                List<DailyForecast> result = ExplorerParser.parseDaily(body);
                mainHandler.post(() -> {
                    if (result == null || result.isEmpty()) {
                        status.setValue(Status.EMPTY);
                    } else {
                        daily.setValue(result);
                        status.setValue(Status.SUCCESS);
                    }
                });
            } catch (HttpClient.HttpStatusException e) {
                postError(ErrorType.HTTP, e.code);
            } catch (SocketTimeoutException e) {
                postError(ErrorType.TIMEOUT, 0);
            } catch (IOException e) {
                postError(ErrorType.NO_CONNECTION, 0);
            } catch (JSONException e) {
                postError(ErrorType.PARSE, 0);
            }
        });
    }

    /** Cancelamento cooperativo (RF4). */
    public void cancel() {
        if (currentTask != null) currentTask.cancel(true);
        // Se ainda está em LOADING, passa a CANCELLED sem tocar nos dados anteriores
        if (status.getValue() == Status.LOADING) {
            mainHandler.post(() -> status.setValue(Status.CANCELLED));
        }
    }

    // ---------- DETALHE (pedido B) ----------
    /** Chamado na UI thread. Carrega as horas de um dia. */
    public void loadDetail(String isoDate) {
        if (status.getValue() == Status.LOADING) return;

        LocationInfo loc = selectedLocation.getValue();
        if (loc == null) loc = new LocationInfo("Porto (UPT campus)", ExplorerApi.PORTO_LAT, ExplorerApi.PORTO_LON);

        final String url = ExplorerApi.hourlyUrl(
                loc.lat, loc.lon, isoDate,
                settings.getValue() != null && settings.getValue().fahrenheit);
        status.setValue(Status.LOADING);
        currentTask = executor.submit(() -> {
            try {
                String body = HttpClient.get(url);
                List<HourlyForecast> result = ExplorerParser.parseHourly(body);
                mainHandler.post(() -> {
                    if (result == null || result.isEmpty()) {
                        status.setValue(Status.EMPTY);
                    } else {
                        hourly.setValue(result);
                        status.setValue(Status.SUCCESS);
                    }
                });
            } catch (HttpClient.HttpStatusException e) {
                postError(ErrorType.HTTP, e.code);
            } catch (SocketTimeoutException e) {
                postError(ErrorType.TIMEOUT, 0);
            } catch (IOException e) {
                postError(ErrorType.NO_CONNECTION, 0);
            } catch (JSONException e) {
                postError(ErrorType.PARSE, 0);
            }
        });
    }

    // ---------- COMPARAÇÃO (3 em paralelo) ----------
    /** Resultado da comparação para um local. */
    public static final class CompareResult {
        public final String label;
        public final List<DailyForecast> data; // null se falhou
        public final ErrorType errorType;       // null se OK
        public final int httpCode;

        public CompareResult(String label, List<DailyForecast> data,
                             ErrorType errorType, int httpCode) {
            this.label = label;
            this.data = data;
            this.errorType = errorType;
            this.httpCode = httpCode;
        }
    }

    /**
     * Corre três pedidos em paralelo com um pool fixo de 3 threads.
     * Usa submit() (não invokeAll) para entregar cada resultado assim que fica pronto.
     */
    public void compare(LiveDataCallback callback) {
        status.setValue(Status.LOADING);
        final int total = 3;
        final int[] completed = {0};
        final CompareResult[] results = new CompareResult[total];

        String[] labels = {"Porto", "Lisboa", "Faro"};
        double[][] coords = {
                {ExplorerApi.PORTO_LAT, ExplorerApi.PORTO_LON},
                {ExplorerApi.LISBOA_LAT, ExplorerApi.LISBOA_LON},
                {ExplorerApi.FARO_LAT, ExplorerApi.FARO_LON}
        };

        for (int i = 0; i < total; i++) {
            final int idx = i;
            comparePool.submit(() -> {
                CompareResult r;
                try {
                    Settings s = settings.getValue();
                    boolean fah = s != null && s.fahrenheit;
                    String url = ExplorerApi.dailyUrl(coords[idx][0], coords[idx][1], 1, fah);
                    String body = HttpClient.get(url);
                    List<DailyForecast> data = ExplorerParser.parseDaily(body);
                    r = new CompareResult(labels[idx], data, null, 0);
                } catch (HttpClient.HttpStatusException e) {
                    r = new CompareResult(labels[idx], null, ErrorType.HTTP, e.code);
                } catch (SocketTimeoutException e) {
                    r = new CompareResult(labels[idx], null, ErrorType.TIMEOUT, 0);
                } catch (IOException e) {
                    r = new CompareResult(labels[idx], null, ErrorType.NO_CONNECTION, 0);
                } catch (JSONException e) {
                    r = new CompareResult(labels[idx], null, ErrorType.PARSE, 0);
                }
                final CompareResult result = r;
                mainHandler.post(() -> {
                    results[idx] = result;
                    completed[0]++;
                    if (completed[0] == total) {
                        callback.onComplete(results);
                        status.setValue(Status.SUCCESS);
                    }
                });
            });
        }
    }

    /** Callback para a Activity receber os três resultados. */
    public interface LiveDataCallback {
        void onComplete(CompareResult[] results);
    }

    // ---------- Favoritos e definições ----------
    public void toggleFavourite(String date) {
        Set<String> current = favourites.getValue();
        if (current == null) current = Collections.emptySet();
        Set<String> copy = new HashSet<>(current);
        if (!copy.add(date)) copy.remove(date);
        favourites.setValue(Collections.unmodifiableSet(copy));
    }

    public void setSettings(Settings newSettings) {
        settings.setValue(newSettings);
    }

    // ---------- Interno ----------
    private void postError(ErrorType type, int httpCode) {
        mainHandler.post(() -> {
            lastHttpCode.setValue(httpCode);
            error.setValue(type);
            status.setValue(Status.ERROR);
            // os dados anteriores mantêm-se (RF16)
        });
    }

    @Override
    protected void onCleared() {
        if (currentTask != null) currentTask.cancel(true);
        executor.shutdownNow();
        comparePool.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
