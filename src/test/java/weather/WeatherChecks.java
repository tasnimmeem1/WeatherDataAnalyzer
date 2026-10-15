package weather;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;

/// Dependency-free regression checks. Example: `java weather.WeatherChecks`.
public interface WeatherChecks {
    /// Verifies sample statistics, boundary values, missing months, and invalid CSV data.
    /// Example: `WeatherChecks.main(new String[0])` from the project directory.
    /// @param args unused
    /// @throws Exception on a failed check or filesystem error
    static void main(String[] args) throws Exception {
        var days = WeatherAnalyzer.parseLines(java.util.List.of(
                "Date,Temperature,Humidity,Precipitation",
                "2023-08-01,32.5,65,0.0", "2023-08-02,35.0,60,0.2",
                "2023-08-03,28.0,72,1.5", "2023-08-04,30.0,68,0.0",
                "2023-08-05,24.5,80,3.2", "2023-09-01,22.0,70,0.0",
                "2023-09-02,14.0,85,2.0", "2024-08-01,40.0,50,0.0"));
        check(Math.abs(WeatherAnalyzer.average(days, YearMonth.of(2023, 8)).orElseThrow() - 30) < 1e-9, "month average");
        check(WeatherAnalyzer.above(days, 30).size() == 3, "strict threshold");
        check(WeatherAnalyzer.rainy(days) == 4, "rain count");
        check(WeatherAnalyzer.average(days, YearMonth.of(2025, 1)).isEmpty(), "missing month");
        check(WeatherAnalyzer.category(15).equals("Warm") && WeatherAnalyzer.category(30).equals("Hot")
                && WeatherAnalyzer.category(14.9).equals("Cold"), "category boundaries");
        rejects(() -> WeatherAnalyzer.parse("2023-02-30,20,50,0", 2));
        rejects(() -> WeatherAnalyzer.parse("2023-08-01,NaN,50,0", 2));
        rejects(() -> WeatherAnalyzer.parse("2023-08-01,20,101,0", 2));
        rejects(() -> WeatherAnalyzer.parse("2023-08-01,20,50,-1", 2));
        rejects(() -> WeatherAnalyzer.parse("2023-08-01,20,50", 2));
        rejects(() -> WeatherAnalyzer.above(days, Double.NaN));
        var temporary = Files.createTempFile("weather-check", ".csv");
        try {
            Files.writeString(temporary, "Date,Temperature,Humidity,Precipitation\n\n2023-08-01,20,50,0\n2023-08-01,21,50,0\n");
            boolean rejected = false;
            try { WeatherAnalyzer.load(temporary); } catch (IllegalArgumentException ex) { rejected = true; }
            check(rejected, "duplicate dates");
            Files.writeString(temporary, "Date,Temperature,Humidity,Precipitation\n\n");
            check(WeatherAnalyzer.load(temporary).isEmpty(), "header-only CSV");
        } finally { Files.deleteIfExists(temporary); }
        check(Math.abs(TemperatureUnit.display(30, "F") - 86) < 1e-9, "Fahrenheit conversion");
        check(Math.abs(TemperatureUnit.celsius(86, "F") - 30) < 1e-9, "Fahrenheit threshold");
        check(TemperatureUnit.display(-40, "F") == -40, "negative temperature");
        check(TemperatureUnit.format(0, "F").equals("32.0 °F"), "unit label");
        check(WeatherAnalyzer.report(days, YearMonth.of(2023, 8), 30, "F").contains("Days above 86.0 °F (entire file): 3"), "Fahrenheit report");
        rejects(() -> TemperatureUnit.normalize("K"));
        var winter = WeatherAnalyzer.parseLines(java.util.List.of(
                "Date,Temperature,Humidity,Precipitation",
                "2026-01-01,-8,50,0", "2026-01-02,-5,50,0",
                "2026-01-03,-2,50,0", "2026-01-04,0,50,0",
                "2026-01-05,4,50,0", "2026-01-06,-10,50,0",
                "2026-01-07,-4,50,0", "2026-01-08,10,50,0"));
        var start = java.time.LocalDate.of(2026, 1, 1);
        var week = winter.stream().filter(d -> !d.date().isBefore(start)
                && d.date().isBefore(start.plusDays(7))).toList();
        check(WeatherAnalyzer.above(week, -5).size() == 4, "negative weekly threshold excludes equal and eighth day");
        check(WeatherAnalyzer.above(week, TemperatureUnit.celsius(23, "F")).size() == 4,
                "equivalent Fahrenheit weekly threshold");
        System.out.println("All weather checks passed.");
    }

    /// Throws when a condition fails. Example: `check(true, "example")`.
    /// @param condition result to verify
    /// @param message failure description
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /// Requires an invalid-input operation to throw. Example: `rejects(() -> WeatherAnalyzer.category(Double.NaN))`.
    /// @param action operation under test
    static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { return; }
        throw new AssertionError("Expected invalid input to be rejected");
    }
}
