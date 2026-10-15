package weather;

import java.util.Locale;

/// Temperature conversions. Stored observations always remain Celsius.
/// Example: `TemperatureUnit.format(30, "F")` returns `"86.0 °F"`.
public interface TemperatureUnit {
    /// Validates a unit. Example: `normalize("f")` returns `"F"`.
    /// @param unit C or F, case insensitive
    /// @return canonical unit
    static String normalize(String unit) {
        return switch (unit.strip().toUpperCase(Locale.ROOT)) {
            case "C", "°C" -> "C";
            case "F", "°F" -> "F";
            default -> throw new IllegalArgumentException("Unit must be C or F");
        };
    }
    /// Converts Celsius for display. Example: `display(0, "F")` returns 32.
    /// @param celsius stored temperature
    /// @param unit display unit
    /// @return converted temperature
    static double display(double celsius, String unit) {
        return normalize(unit).equals("F") ? celsius * 9.0 / 5.0 + 32 : celsius;
    }
    /// Converts an entered threshold to Celsius. Example: `celsius(86, "F")` returns 30.
    /// @param value entered threshold
    /// @param unit entered unit
    /// @return Celsius threshold
    static double celsius(double value, String unit) {
        return normalize(unit).equals("F") ? (value - 32) * 5.0 / 9.0 : value;
    }
    /// Formats a reading. Example: `format(30, "F")` returns `"86.0 °F"`.
    /// @param celsius stored temperature
    /// @param unit display unit
    /// @return labeled reading
    static String format(double celsius, String unit) {
        return String.format(Locale.ROOT, "%.1f °%s", display(celsius, unit), normalize(unit));
    }
}
