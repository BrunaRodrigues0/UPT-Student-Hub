package pt.upt.studenthub;

/**
 * Modelo imutável com os dados mostrados no ecrã.
 * Campos final: uma vez criado não muda, logo é seguro passar entre threads.
 */
public final class WeatherReport {

    public final double temperature; // °C
    public final int humidity;       // %
    public final double windSpeed;   // km/h
    public final int weatherCode;    // WMO
    public final String time;        // "2026-10-15T10:45"

    public WeatherReport(double temperature, int humidity, double windSpeed,
                         int weatherCode, String time) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.time = time;
    }

    /** "2026-10-15T10:45" -> "10:45". */
    public String hour() {
        int t = time.indexOf('T');
        return t >= 0 ? time.substring(t + 1) : time;
    }
}
