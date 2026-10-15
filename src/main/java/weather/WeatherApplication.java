package weather;

import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import java.time.format.DateTimeFormatter;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/// Starts the FXML dashboard and handles actions through lambdas.
/// Example: run `mvn javafx:run`.
public interface WeatherApplication {
    /// Starts JavaFX. Example: `WeatherApplication.main(new String[0])`.
    /// @param args unused
    static void main(String[] args) { Platform.startup(WeatherApplication::show); }

    /// Loads the bundled CSV automatically, independent of the working directory.
    /// Example: `var days = WeatherApplication.loadData()`.
    /// @return validated daily observations
    /// @throws IOException when the bundled data is unavailable
    static List<WeatherData> loadData() throws IOException {
        var stream = WeatherApplication.class.getResourceAsStream("weatherdata.csv");
        if (stream == null) throw new IOException("Weather data is unavailable");
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return WeatherAnalyzer.parseLines(reader.lines().toList());
        }
    }

    /// Loads FXML, connects controls, and displays initial results.
    /// Example: called by `Platform.startup(WeatherApplication::show)`.
    static void show() {
        try {
            var loader = new FXMLLoader(WeatherApplication.class.getResource("weather-view.fxml"));
            Parent root = loader.load();
            var controls = loader.getNamespace();
            var output = (TextArea) controls.get("outputArea");
            var status = (Label) controls.get("statusLabel");
            var average = (Label) controls.get("averageValue");
            var category = (Label) controls.get("categoryValue");
            var rainy = (Label) controls.get("rainyValue");
            var hot = (Label) controls.get("aboveValue");
            var aboveCaption = (Label) controls.get("aboveCaption");
            var sceneDates = (DatePicker) controls.get("sceneDatePicker");
            var description = (Label) controls.get("sceneDescription");
            var sunScene = (Node) controls.get("sunScene");
            var cloudScene = (Node) controls.get("cloudScene");
            var rainScene = (Node) controls.get("rainScene");
            var snowScene = (Node) controls.get("snowScene");
            var sun = (Node) controls.get("sceneSun");
            var pulse = new ScaleTransition(Duration.seconds(2), sun);
            pulse.setFromX(0.92); pulse.setFromY(0.92);
            pulse.setToX(1.08); pulse.setToY(1.08);
            pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE);
            var drops = java.util.stream.IntStream.range(0, 8).mapToObj(i -> {
                var drop = (Node) controls.get("drop" + i);
                var motion = new TranslateTransition(Duration.seconds(0.8 + i * 0.06), drop);
                motion.setFromY(-5); motion.setToY(43);
                motion.setDelay(Duration.seconds(i * 0.09));
                motion.setCycleCount(Animation.INDEFINITE);
                return motion;
            }).toList();
            var flakes = java.util.stream.IntStream.range(0, 8).mapToObj(i -> {
                var flake = (Node) controls.get("flake" + i);
                var motion = new TranslateTransition(Duration.seconds(2.1 + i * 0.12), flake);
                motion.setFromY(-10); motion.setToY(42);
                motion.setFromX(-80 + i * 24); motion.setToX(-70 + i * 24);
                motion.setDelay(Duration.seconds(i * 0.18));
                motion.setCycleCount(Animation.INDEFINITE);
                return motion;
            }).toList();
            var thresholdField = (TextField) controls.get("thresholdField");
            thresholdField.setText("30");
            var unitPicker = (ComboBox<String>) controls.get("unitPicker");
            unitPicker.getItems().setAll("°C", "°F"); unitPicker.setValue("°C");
            var temperatureHeader = (Label) controls.get("temperatureHeader");
            var forecastRows = (VBox) controls.get("forecastRows");
            var chartCaption = (Label) controls.get("chartCaption");
            try {
                var days = loadData();
                if (!days.isEmpty()) sceneDates.setValue(days.stream().map(WeatherData::date).min(LocalDate::compareTo).orElseThrow());
                Runnable updateScene = () -> {
                    pulse.stop(); drops.forEach(Animation::stop); flakes.forEach(Animation::stop);
                    sunScene.setVisible(false); cloudScene.setVisible(false); rainScene.setVisible(false); snowScene.setVisible(false);
                    days.stream().filter(d -> d.date().equals(sceneDates.getValue())).findFirst().ifPresent(day -> {
                        if (day.temperature() <= 0) {
                            snowScene.setVisible(true); flakes.forEach(Animation::playFromStart);
                            description.setText("Freezing • " + TemperatureUnit.format(day.temperature(), unitPicker.getValue()));
                        } else if (day.precipitation() > 0) {
                            rainScene.setVisible(true); drops.forEach(Animation::playFromStart);
                            description.setText(String.format(Locale.ROOT, "Rainy • %.1f mm rainfall • %s", day.precipitation(), TemperatureUnit.format(day.temperature(), unitPicker.getValue())));
                        } else if (WeatherAnalyzer.category(day.temperature()).equals("Cold")) {
                            cloudScene.setVisible(true);
                            description.setText("Cold & dry • " + TemperatureUnit.format(day.temperature(), unitPicker.getValue()));
                        } else {
                            sunScene.setVisible(true); pulse.playFromStart();
                            description.setText(WeatherAnalyzer.category(day.temperature()) + " & dry • " + TemperatureUnit.format(day.temperature(), unitPicker.getValue()));
                        }
                    });
                };
                updateScene.run();
                Runnable update = () -> {
                    try {
                        var unit = unitPicker.getValue();
                        temperatureHeader.setText("TEMPERATURE (" + unit + ")");
                        var date = sceneDates.getValue();
                        if (date == null) throw new IllegalArgumentException("Choose a calendar date");
                        var selected = YearMonth.from(date);
                        var observation = days.stream().filter(d -> d.date().equals(date)).findFirst();
                        int length = 7;
                        double enteredThreshold = Double.parseDouble(thresholdField.getText().strip());
                        double threshold = TemperatureUnit.celsius(enteredThreshold, unit);
                        if (!Double.isFinite(threshold))
                            throw new IllegalArgumentException("Enter a finite threshold, including negative values.");
                        var window = days.stream().filter(d -> date != null && !d.date().isBefore(date)
                                && d.date().isBefore(date.plusDays(length)))
                                .sorted(java.util.Comparator.comparing(WeatherData::date)).toList();
                        forecastRows.getChildren().clear();
                        window.forEach(d -> forecastRows.getChildren().add(dayRow(d, date, unit)));
                        chartCaption.setText(window.isEmpty() ? "No readings in this date range"
                                : date + " • " + window.size() + " days available");
                        String report;
                        boolean visible = true;
                        if (observation.isEmpty()) {
                            report = "No weather data for " + date + ". Choose a date included in the sample year.";
                            visible = false;
                        } else {
                            report = WeatherAnalyzer.dailyReport(observation.get(), unit);
                        }
                        updateScene.run();
                        if (!visible) {
                            sunScene.setVisible(false); cloudScene.setVisible(false);
                            rainScene.setVisible(false); snowScene.setVisible(false);
                            pulse.stop(); drops.forEach(Animation::stop); flakes.forEach(Animation::stop);
                            description.setText("No data for selected date");
                        }
                        var mean = window.stream().mapToDouble(WeatherData::temperature).average();
                        average.setText(mean.isPresent() ? String.format(Locale.ROOT, "%.2f %s", TemperatureUnit.display(mean.getAsDouble(), unit), unit) : "No data");
                        category.setText(mean.isPresent() ? WeatherAnalyzer.category(mean.getAsDouble()) + " weekly average" : "No observations");
                        rainy.setText(Long.toString(WeatherAnalyzer.rainy(window)));
                        hot.setText(Integer.toString(WeatherAnalyzer.above(window, threshold).size()));
                        aboveCaption.setText("Above " + TemperatureUnit.format(threshold, unit) + " · displayed 7 days");
                        var monthlyMean = WeatherAnalyzer.average(days, selected);
                        String monthTitle = selected.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
                        String monthlyReading = monthlyMean.isPresent()
                                ? String.format(Locale.ROOT, "%.2f %s",
                                        TemperatureUnit.display(monthlyMean.getAsDouble(), unit), unit)
                                : "No data";
                        output.setText(report + "\nMonthly average\n" + monthTitle + "\n" + monthlyReading);
                        status.setText(""); status.setVisible(false); status.setManaged(false);
                    } catch (IllegalArgumentException ex) {
                        average.setText("—"); rainy.setText("—"); hot.setText("—");
                        category.setText("Check your selection"); aboveCaption.setText("Days above threshold");
                        output.clear();
                        pulse.stop(); drops.forEach(Animation::stop); flakes.forEach(Animation::stop);
                        sunScene.setVisible(false); cloudScene.setVisible(false); rainScene.setVisible(false); snowScene.setVisible(false);
                        description.setText("Choose a date");
                        status.setText("Choose a valid date and enter a numeric threshold (negative values are allowed)."); status.setVisible(true); status.setManaged(true);
                    }
                };
                sceneDates.setOnAction(e -> update.run());
                thresholdField.textProperty().addListener((observable, previous, current) -> update.run());
                unitPicker.valueProperty().addListener((observable, previous, current) -> {
                    try {
                        double value = Double.parseDouble(thresholdField.getText().strip());
                        double celsius = TemperatureUnit.celsius(value, previous);
                        double converted = TemperatureUnit.display(celsius, current);
                        thresholdField.setText(java.math.BigDecimal.valueOf(converted)
                                .setScale(10, java.math.RoundingMode.HALF_UP)
                                .stripTrailingZeros().toPlainString());
                    } catch (IllegalArgumentException ex) {
                        // Keep invalid input visible so the user can correct it.
                    }
                    update.run();
                });
                update.run();
            } catch (IOException | IllegalArgumentException ex) {
                status.setText("Weather data could not be loaded: " + ex.getMessage()); status.setVisible(true); status.setManaged(true);
            }
            var stage = new Stage();
            stage.setTitle("Weather Data Analyzer");
            stage.setMinWidth(1000); stage.setMinHeight(760);
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> { pulse.stop(); drops.forEach(Animation::stop); flakes.forEach(Animation::stop); });
            stage.show();
        } catch (IOException ex) {
            System.err.println("Cannot load weather screen: " + ex.getMessage());
            Platform.exit();
        }
    }
    /// Loads one FXML row for the weather-style daily list.
    /// Rain uses precipitation in millimeters, not a forecast probability.
    /// Example: `dayRow(days.getFirst(), days.getFirst().date())`.
    /// @param day daily observation
    /// @param selected first date in the displayed range
    /// @return FXML row with populated labels and temperature bar
    static Parent dayRow(WeatherData day, LocalDate selected) {
        return dayRow(day, selected, "C");
    }

    /// Loads a daily row in the selected unit. Example: `dayRow(day, day.date(), "F")`.
    /// @param day observation
    /// @param selected first date
    /// @param unit C or F
    /// @return populated row
    static Parent dayRow(WeatherData day, LocalDate selected, String unit) {
        try {
            var loader = new FXMLLoader(WeatherApplication.class.getResource("weather-day.fxml"));
            Parent row = loader.load();
            var fields = loader.getNamespace();
            ((Label) fields.get("dayLabel")).setText(day.date().format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)));
            ((Label) fields.get("dateLabel")).setText(day.date().format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)));
            var icon = (Label) fields.get("weatherIcon");
            if (day.temperature() <= 0) { icon.setText("❄"); icon.getStyleClass().add("snow-icon"); }
            else if (day.precipitation() > 0) { icon.setText("☂"); icon.getStyleClass().add("rain-icon"); }
            else if (day.temperature() < 15) { icon.setText("☁"); }
            else { icon.setText("☀"); icon.getStyleClass().add("sun-icon"); }
            ((Label) fields.get("rainLabel")).setText(String.format(Locale.ROOT, "%.1f mm", day.precipitation()));
            ((Label) fields.get("temperatureLabel")).setText(TemperatureUnit.format(day.temperature(), unit));
            ((ProgressBar) fields.get("temperatureBar")).setProgress(Math.max(0, Math.min(1, (day.temperature() + 10) / 50)));
            return row;
        } catch (IOException ex) {
            throw new java.io.UncheckedIOException("Cannot load daily weather row", ex);
        }
    }

}
