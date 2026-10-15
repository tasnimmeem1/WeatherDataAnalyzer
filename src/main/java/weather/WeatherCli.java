package weather;

import java.nio.file.Path;
import java.time.YearMonth;

/// Command-line entry point. Example: `java weather.WeatherCli weatherdata.csv 2023-08 30`.
public interface WeatherCli {
    /// Prints a report; exits with status 1 on invalid input.
    /// Example arguments: `weatherdata.csv 2023-08 30`.
    /// @param args optional file, month, threshold in selected unit, and C or F
    static void main(String[] args) {
        try {
            if (args.length > 4) throw new IllegalArgumentException("Usage: WeatherCli [file] [YYYY-MM] [threshold] [C|F]");
            var days = WeatherAnalyzer.load(Path.of(args.length > 0 ? args[0] : "weatherdata.csv"));
            var month = YearMonth.parse(args.length > 1 ? args[1] : "2026-08");
            String unit = TemperatureUnit.normalize(args.length > 3 ? args[3] : "C");
            double threshold = args.length > 2
                    ? TemperatureUnit.celsius(Double.parseDouble(args[2]), unit) : 30;
            System.out.print(WeatherAnalyzer.report(days, month, threshold, unit));
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
        }
    }
}
