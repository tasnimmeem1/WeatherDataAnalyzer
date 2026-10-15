package weather;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.IntStream;

/// Functional CSV parsing and weather statistics with no mutable shared state.
/// ```java
/// var days = WeatherAnalyzer.load(Path.of("weatherdata.csv"));
/// var average = WeatherAnalyzer.average(days, YearMonth.of(2023, 8));
/// ```
public interface WeatherAnalyzer {
    /// Loads UTF-8 CSV with the exact header `Date,Temperature,Humidity,Precipitation`.
    /// Blank lines are ignored; dates must be unique. Example: `load(Path.of("weatherdata.csv"))`.
    /// @param path CSV path
    /// @return immutable observations
    /// @throws IOException if the file cannot be read
    /// @throws IllegalArgumentException if the header, row, or date uniqueness is invalid
    static List<WeatherData> load(Path path) throws IOException {
        return parseLines(Files.readAllLines(path));
    }

    /// Parses already-read CSV lines with the same validation as file loading.
    /// Example: `parseLines(List.of("Date,Temperature,Humidity,Precipitation"))`.
    /// @param lines header and observation rows
    /// @return immutable validated observations
    static List<WeatherData> parseLines(List<String> lines) {
        if (lines.isEmpty() || !lines.get(0).replace("\uFEFF", "").strip()
                .equals("Date,Temperature,Humidity,Precipitation"))
            throw new IllegalArgumentException("Expected header: Date,Temperature,Humidity,Precipitation");
        var days = IntStream.range(1, lines.size())
                .filter(i -> !lines.get(i).isBlank())
                .mapToObj(i -> parse(lines.get(i), i + 1)).toList();
        var dates = new HashSet<LocalDate>();
        days.forEach(day -> {
            if (!dates.add(day.date())) throw new IllegalArgumentException("Duplicate date: " + day.date());
        });
        return days;
    }

    /// Parses an unquoted four-column numeric row and includes its line number in errors.
    /// Example: `parse("2023-08-01,32.5,65,0.0", 2)`.
    /// @param line CSV row
    /// @param lineNumber original file line number
    /// @return validated observation
    static WeatherData parse(String line, int lineNumber) {
        try {
            var fields = line.split(",", -1);
            if (fields.length != 4) throw new IllegalArgumentException("Expected four columns");
            return new WeatherData(LocalDate.parse(fields[0].strip()),
                    Double.parseDouble(fields[1].strip()), Double.parseDouble(fields[2].strip()),
                    Double.parseDouble(fields[3].strip()));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Line " + lineNumber + ": " + ex.getMessage(), ex);
        }
    }

    /// Computes the mean for one year and month, avoiding mixing years.
    /// Example: `average(days, YearMonth.of(2023, 8))`.
    /// @param days observations
    /// @param month selected year-month
    /// @return empty if no matching records exist
    static OptionalDouble average(List<WeatherData> days, YearMonth month) {
        return days.stream().filter(d -> YearMonth.from(d.date()).equals(month))
                .mapToDouble(WeatherData::temperature).average();
    }

    /// Returns days **strictly above** the threshold, sorted by date.
    /// Example: `above(days, 30)` excludes exactly 30 degrees.
    /// @param days observations
    /// @param threshold finite Celsius threshold
    /// @return immutable matching days
    static List<WeatherData> above(List<WeatherData> days, double threshold) {
        if (!Double.isFinite(threshold)) throw new IllegalArgumentException("Threshold must be finite");
        return days.stream().filter(d -> d.temperature() > threshold)
                .sorted(Comparator.comparing(WeatherData::date)).toList();
    }

    /// Counts days with precipitation **greater than zero** across the loaded file.
    /// Example: `rainy(days)`.
    /// @param days observations
    /// @return rainy day count
    static long rainy(List<WeatherData> days) {
        return days.stream().filter(d -> d.precipitation() > 0).count();
    }

    /// Uses an enhanced switch expression: Cold below 15, Warm from 15 to below 30, Hot at 30 or above.
    /// Example: `category(32.5)` returns `"Hot"`.
    /// @param temperature finite Celsius temperature
    /// @return weather category
    static String category(double temperature) {
        if (!Double.isFinite(temperature)) throw new IllegalArgumentException("Temperature must be finite");
        int band = temperature >= 30 ? 2 : temperature >= 15 ? 1 : 0;
        return switch (band) { case 2 -> "Hot"; case 1 -> "Warm"; default -> "Cold"; };
    }

    /// Formats a report using a Java 15 text block and `formatted`.
    /// Example: `report(days, YearMonth.of(2023, 8), 30)`.
    /// @param days observations
    /// @param month average-temperature month
    /// @param threshold Celsius threshold
    /// @return readable summary and matching dates
    static String report(List<WeatherData> days, YearMonth month, double threshold) {
        var mean = average(days, month);
        var matches = above(days, threshold);
        String rows = matches.stream().map(d -> "%s  |  %.1f °C  |  %s".formatted(
                d.date(), d.temperature(), category(d.temperature())))
                .collect(java.util.stream.Collectors.joining("\n"));
        return """
                WEATHER DATA ANALYZER
                Loaded observations: %d
                Average for %s: %s
                Rainy days (entire file): %d
                Days above %.1f °C (entire file): %d

                %s
                """.formatted(days.size(), month,
                mean.isPresent() ? "%.2f °C".formatted(mean.getAsDouble()) : "No data",
                rainy(days), threshold, matches.size(), rows.isEmpty() ? "No matching days." : rows);
    }
    /// Formats all readings for one selected day with a Java text block.
    /// Example: `dailyReport(days.getFirst())` on a nonempty list.
    /// @param day selected observation
    /// @return date, temperature, humidity, precipitation, and category
    static String dailyReport(WeatherData day) {
        return """
                %s

                Temperature: %.1f °C
                Humidity: %.0f %%
                Precipitation: %.1f mm
                Category: %s
                """.formatted(day.date(), day.temperature(), day.humidity(),
                        day.precipitation(), category(day.temperature()));
    }

    /// Generates a report in the selected unit while filtering stored Celsius values.
    /// Example: `report(days, YearMonth.of(2026, 8), 30, "F")`.
    /// @param days observations
    /// @param month average month
    /// @param threshold Celsius cutoff
    /// @param unit C or F
    /// @return formatted report
    static String report(List<WeatherData> days, YearMonth month, double threshold, String unit) {
        TemperatureUnit.normalize(unit);
        var mean = average(days, month);
        var matches = above(days, threshold);
        var rows = matches.stream().map(d -> "%s | %s | %s".formatted(d.date(),
                TemperatureUnit.format(d.temperature(), unit), category(d.temperature())))
                .collect(java.util.stream.Collectors.joining("\n"));
        return """
                WEATHER DATA ANALYZER
                Loaded observations: %d
                Average for %s: %s
                Rainy days (entire file): %d
                Days above %s (entire file): %d

                %s
                """.formatted(days.size(), month,
                mean.isPresent() ? TemperatureUnit.format(mean.getAsDouble(), unit) : "No data",
                rainy(days), TemperatureUnit.format(threshold, unit), matches.size(),
                rows.isEmpty() ? "No matching days." : rows);
    }

    /// Formats daily readings in the selected unit. Example: `dailyReport(day, "F")`.
    /// @param day observation
    /// @param unit C or F
    /// @return daily report
    static String dailyReport(WeatherData day, String unit) {
        return """
                %s

                Temperature: %s
                Humidity: %.0f %%
                Precipitation: %.1f mm
                Category: %s
                """.formatted(day.date(), TemperatureUnit.format(day.temperature(), unit),
                day.humidity(), day.precipitation(), category(day.temperature()));
    }

}
