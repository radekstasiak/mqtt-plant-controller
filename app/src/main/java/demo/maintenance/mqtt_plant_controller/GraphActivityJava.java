package demo.maintenance.mqtt_plant_controller;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;
import demo.maintenance.mqtt_plant_controller.networking.GrowApiService;
import demo.maintenance.mqtt_plant_controller.networking.entity.Reading;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.util.ArrayList;
import java.util.List;

public class GraphActivityJava extends AppCompatActivity implements Callback<List<Reading>> {
    private final String TAG = GraphActivityJava.class.toString() + "Debug";

    private String BASE_URL = "https://grow-mqtt-readings.herokuapp.com/";
    private GrowApiService growApiService;
    private AnyChartView anyChartView;
    private ArrayList<DataEntry> seriesDataEntry = new ArrayList<DataEntry>();
    private Cartesian cartesian;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .build();

        growApiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(GrowApiService.class);

        Call<List<Reading>> readings = growApiService.getReadings();
        readings.enqueue(this);
        anyChartView = findViewById(R.id.any_chart_view);
        anyChartView.setProgressBar(findViewById(R.id.progress_bar));

        cartesian = AnyChart.line();
        cartesian.animation(true);

        cartesian.padding(10d, 20d, 5d, 20d);

        cartesian.crosshair().enabled(true);
        cartesian.crosshair()
                .yLabel(true)
                // TODO ystroke
                .yStroke((Stroke) null, null, null, (String) null, (String) null);

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        cartesian.title("Soil Moisture Level");

        cartesian.yAxis(0).title("Moisture level raw value");
        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);


    }

    @Override
    public void onResponse(Call<List<Reading>> call, Response<List<Reading>> response) {
        ArrayList<Reading> readings = new ArrayList(response.body());
        for (Reading reading : readings) {
            seriesDataEntry.add(new ValueDataEntry(reading.getCreatedAt(), reading.getMoistureLevelStatus()));
        }

        Set set = Set.instantiate();
        set.data(seriesDataEntry);
        Mapping series1Mapping = set.mapAs("{ x: 'x', value: 'value' }");
        Line series1 = cartesian.line(series1Mapping);
        series1.name("Test Plant");
        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);
        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(13d);
        cartesian.legend().padding(0d, 0d, 10d, 0d);

        anyChartView.setChart(cartesian);

    }

    @Override
    public void onFailure(Call<List<Reading>> call, Throwable t) {

    }
}
