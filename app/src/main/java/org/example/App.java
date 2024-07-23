package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class App extends Application {
    private static final Queue<StockDataPoint> stockQueue = new LinkedList<>();
    private static final OkHttpClient client = new OkHttpClient();
    private static final String API_KEY = "YOUR_ALPHA_VANTAGE_API_KEY";
    private static XYChart.Series<Number, Number> series;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Stock Price Monitor");

        // Defining the x and y axes
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Price");

        // Creating the line chart
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Stock Monitoring, 2024");

        // Defining a series to display data
        series = new XYChart.Series<>();
        series.setName("Google");

        lineChart.getData().add(series);

        // Setting up the scene
        Scene scene = new Scene(lineChart, 800, 600);
        stage.setScene(scene);
        stage.show();

        // Schedule the stock price updates
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    double price = getStockPrice();
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    StockDataPoint dataPoint = new StockDataPoint(stockQueue.size(), price);
                    stockQueue.add(dataPoint);
                    Platform.runLater(() -> series.getData().add(new XYChart.Data<>(dataPoint.getTime(), dataPoint.getPrice())));
                    System.out.println("Timestamp: " + timestamp + ", Price: " + price);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }, 0, 60000);
    }

    private static double getStockPrice() throws IOException, Exception {
        String url = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=GOOG&interval=1min&apikey=" + API_KEY;
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            JSONObject jsonObject = new JSONObject(responseBody);

            if (jsonObject.has("Time Series (1min)")) {
                JSONObject timeSeries = jsonObject.getJSONObject("Time Series (1min)");
                String latestTime = timeSeries.keys().next();
                JSONObject latestData = timeSeries.getJSONObject(latestTime);

                return latestData.getDouble("4. close");
            } else if (jsonObject.has("Note")) {
                throw new Exception("API call frequency is greater than the allowed limit.");
            } else if (jsonObject.has("Error Message")) {
                throw new Exception("Invalid API call. Please check the API call and try again.");
            } else {
                throw new Exception("Time Series (1min) data not available in the response");
            }
        }
    }

    // Helper class to store stock data points
    public static class StockDataPoint {
        private final int time;
        private final double price;

        public StockDataPoint(int time, double price) {
            this.time = time;
            this.price = price;
        }

        public int getTime() {
            return time;
        }

        public double getPrice() {
            return price;
        }
    }
}
