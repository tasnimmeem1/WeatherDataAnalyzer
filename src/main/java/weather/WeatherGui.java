package weather;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.time.YearMonth;

/// JavaFX GUI launched through an interface, without an Application subclass.
/// Example: run `mvn javafx:run` from the project directory.
public interface WeatherGui {
    /// Starts JavaFX and displays the analyzer.
    /// Example: `WeatherGui.main(new String[0])`.
    /// @param args unused arguments
    static void main(String[] args) { Platform.startup(WeatherGui::show); }

    /// Builds the CSS-styled window; all input is validated before analysis.
    /// Example: called by `Platform.startup(WeatherGui::show)`.
    static void show() {
        var stage = new Stage();
        var title = new Label("Weather Data Analyzer");
        title.getStyleClass().add("title");
        var subtitle = new Label("Explore daily weather with modern Java");
        var file = new TextField("weatherdata.csv");
        var browse = new Button("Browse CSV");
        browse.setOnAction(e -> {
            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            var selected = chooser.showOpenDialog(stage);
            if (selected != null) file.setText(selected.getAbsolutePath());
        });
        var month = new TextField("2023-08");
        var threshold = new TextField("30");
        var output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);
        var status = new Label("Select a CSV file, then analyze.");
        var analyze = new Button("Analyze Weather");
        analyze.getStyleClass().add("primary");
        analyze.setOnAction(e -> {
            try {
                var days = WeatherAnalyzer.load(Path.of(file.getText().strip()));
                output.setText(WeatherAnalyzer.report(days, YearMonth.parse(month.getText().strip()),
                        Double.parseDouble(threshold.getText().strip())));
                status.setText("Analysis complete • " + days.size() + " observations");
            } catch (Exception ex) {
                output.clear();
                status.setText("Please correct the input: " + ex.getMessage());
            }
        });
        var pathRow = new HBox(10, file, browse);
        HBox.setHgrow(file, Priority.ALWAYS);
        var filters = new HBox(16, new VBox(6, new Label("Month (YYYY-MM)"), month),
                new VBox(6, new Label("Threshold (°C)"), threshold));
        var root = new VBox(14, title, subtitle, new Label("Weather CSV"), pathRow,
                filters, analyze, status, output);
        root.setPadding(new Insets(28));
        VBox.setVgrow(output, Priority.ALWAYS);
        var scene = new Scene(root, 760, 620);
        scene.getStylesheets().add(WeatherGui.class.getResource("weather.css").toExternalForm());
        stage.setTitle("Weather Data Analyzer");
        stage.setMinWidth(600); stage.setMinHeight(500);
        stage.setScene(scene); stage.show();
    }
}
