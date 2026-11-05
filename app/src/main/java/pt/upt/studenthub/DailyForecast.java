package pt.upt.studenthub;

/**
 * Modelo imutável de um dia de previsão (lista do Módulo 3, variante V1).
 * Campos final: seguro para passar entre threads.
 */
public final class DailyForecast {

    public final String date;             // ISO "2026-11-03"
    public final int weatherCode;         // WMO
    public final double tempMax;          // °C
    public final double tempMin;          // °C
    public final int rainProbability;     // %

    public DailyForecast(String date, int weatherCode, double tempMax,
                         double tempMin, int rainProbability) {
        this.date = date;
        this.weatherCode = weatherCode;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
        this.rainProbability = rainProbability;
    }
}
