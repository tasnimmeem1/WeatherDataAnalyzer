package weather;

import java.time.LocalDate;
import java.util.Objects;

/// One daily observation. Temperature is Celsius; precipitation is millimeters.
/// ```java
/// var day = new WeatherData(LocalDate.of(2023, 8, 1), 32.5, 65, 0);
/// ```
public record WeatherData(LocalDate date, double temperature, double humidity, double precipitation) {
    /// Validates an observation, rejecting nonfinite numbers and invalid ranges.
    /// Example: `new WeatherData(LocalDate.now(), 20, 50, 0)`.
    public WeatherData {
        Objects.requireNonNull(date, "Date is required");
        if (!Double.isFinite(temperature) || !Double.isFinite(humidity)
                || !Double.isFinite(precipitation) || humidity < 0 || humidity > 100 || precipitation < 0)
            throw new IllegalArgumentException("Use finite numbers, humidity 0–100, and precipitation >= 0");
    }
}
