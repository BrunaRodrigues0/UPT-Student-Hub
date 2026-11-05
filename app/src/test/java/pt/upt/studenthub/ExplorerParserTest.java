package pt.upt.studenthub;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes do ExplorerParser com JSON de exemplo (Anexo B — bónus +0,5).
 * Não precisa de Android: corre na JVM (testImplementation JUnit).
 */
public class ExplorerParserTest {

    private static final String DAILY_JSON =
            "{\"daily\":{" +
                    "\"time\":[\"2026-11-03\",\"2026-11-04\"]," +
                    "\"weather_code\":[3,61]," +
                    "\"temperature_2m_max\":[17.3,15.0]," +
                    "\"temperature_2m_min\":[11.0,10.1]," +
                    "\"precipitation_probability_max\":[20,80]}}";

    private static final String HOURLY_JSON =
            "{\"hourly\":{" +
                    "\"time\":[\"2026-11-03T00:00\",\"2026-11-03T01:00\"]," +
                    "\"temperature_2m\":[12.0,11.8]," +
                    "\"precipitation_probability\":[0,5]," +
                    "\"weather_code\":[1,2]}}";

    @Test
    public void parseDaily_readsAllFields() throws Exception {
        List<DailyForecast> days = ExplorerParser.parseDaily(DAILY_JSON);
        assertNotNull(days);
        assertEquals(2, days.size());

        DailyForecast d0 = days.get(0);
        assertEquals("2026-11-03", d0.date);
        assertEquals(3, d0.weatherCode);
        assertEquals(17.3, d0.tempMax, 0.001);
        assertEquals(11.0, d0.tempMin, 0.001);
        assertEquals(20, d0.rainProbability);

        DailyForecast d1 = days.get(1);
        assertEquals("2026-11-04", d1.date);
        assertEquals(61, d1.weatherCode);
        assertEquals(80, d1.rainProbability);
    }

    @Test
    public void parseDaily_handlesMissingArrays() throws Exception {
        String json = "{\"daily\":{\"time\":[\"2026-11-03\"]}}";
        List<DailyForecast> days = ExplorerParser.parseDaily(json);
        assertEquals(1, days.size());
        assertEquals(0, days.get(0).weatherCode);
        assertEquals(0.0, days.get(0).tempMax, 0.001);
        assertEquals(0.0, days.get(0).tempMin, 0.001);
        assertEquals(0, days.get(0).rainProbability);
    }

    @Test
    public void parseHourly_readsAllFields() throws Exception {
        List<HourlyForecast> hours = ExplorerParser.parseHourly(HOURLY_JSON);
        assertNotNull(hours);
        assertEquals(2, hours.size());
        assertEquals("00:00", hours.get(0).hour());
        assertEquals(12.0, hours.get(0).temperature, 0.001);
        assertEquals(1, hours.get(0).weatherCode);
        assertEquals("01:00", hours.get(1).hour());
        assertEquals(5, hours.get(1).rainProbability);
    }

    @Test
    public void parseDaily_returnsEmptyOnNoTimes() throws Exception {
        String json = "{\"daily\":{\"time\":[]}}";
        List<DailyForecast> days = ExplorerParser.parseDaily(json);
        assertTrue(days.isEmpty());
    }
}
