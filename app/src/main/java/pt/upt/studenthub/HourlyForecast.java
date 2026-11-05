package pt.upt.studenthub;

/**
 * Modelo imutável de uma hora de previsão (detalhe da V1).
 */
public final class HourlyForecast {

    public final String time;          // ISO "2026-11-03T14:00"
    public final double temperature;   // °C
    public final int rainProbability;  // %
    public final int weatherCode;      // WMO

    public HourlyForecast(String time, double temperature,
                          int rainProbability, int weatherCode) {
        this.time = time;
        this.temperature = temperature;
        this.rainProbability = rainProbability;
        this.weatherCode = weatherCode;
    }

    /** "2026-11-03T14:00" -> "14:00". */
    public String hour() {
        int t = time.indexOf('T');
        return t >= 0 ? time.substring(t + 1) : time;
    }
}
