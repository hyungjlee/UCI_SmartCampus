package com.example.hjlee.map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.listener.ComboLineColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;
import lecho.lib.hellocharts.view.ComboLineColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

public class informationActivity extends ActionBarActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.information);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
        }



        Intent intent = getIntent();
        String text = intent.getStringExtra("text");
        Log.d("d", text);


    }//oncreate


    public static class PlaceholderFragment extends Fragment {

        private LineChartView chart;
        private LineChartData data;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tempo_chart, container, false);

            chart = (LineChartView) rootView.findViewById(R.id.chart);

            generateTempoData();

            return rootView;
        }

        private void generateTempoData() {
            // I got speed in range (0-50) and height in meters in range(200 - 300). I want this chart to display both
            // information. Differences between speed and height values are large and chart doesn't look good so I need
            // to modify height values to be in range of speed values.

            // The same for displaying Tempo/Height chart.

            float minHeight = 0;
            float maxHeight = 90;
            float tempoRange = 90; // from 0min/km to 15min/km

            float scale = tempoRange / maxHeight;//   0.05
            float sub = (minHeight * scale) / 2;//    10/2

            int numValues = 360;

            Line line;
            List<PointValue> values;
            List<Line> lines = new ArrayList<Line>();

            // Height line, add it as first line to be drawn in the background.
            values = new ArrayList<PointValue>();
            for (int i = 0; i < numValues; ++i) {
                // Some random height values, add +200 to make line a little more natural

               ///////////////////////////////////////////////////////////////
                float rawHeight = (float) (30);
                //////////////////////////////////////////////////////////////////

                float normalizedHeight = rawHeight * scale - sub;//   rawheight 300 = 10
                values.add(new PointValue(i,normalizedHeight ));
            }

            line = new Line(values);
            line.setColor(Color.GRAY);
            line.setHasPoints(false);
            line.setFilled(true);
            line.setStrokeWidth(1);
            lines.add(line);

            // Tempo line is a little tricky because worse tempo means bigger value for example 11min per km is worse
            // than 2min per km but the second should be higher on the chart. So you need to know max tempo and
            // tempoRange and set
            // chart values to minTempo - realTempo.
            values = new ArrayList<PointValue>();
            for (int i = 0; i < numValues; ++i) {
                // Some random raw tempo values.
                float realTempo = (float) Math.random() * 6 + 2;
                float revertedTempo = tempoRange - realTempo;
                values.add(new PointValue(i, revertedTempo));
            }

            line = new Line(values);
            line.setColor(ChartUtils.COLOR_RED);
            line.setHasPoints(false);
            line.setStrokeWidth(1);
            lines.add(line);


            //////////////////////////////////////////////////////////////////
            // Data and axes
            data = new LineChartData(lines);

            // Distance axis(bottom X) with formatter that will ad [km] to values, remember to modify max label charts
            // value.
            Axis distanceAxis = new Axis();
            distanceAxis.setName("Azimuth");
            distanceAxis.setTextColor(ChartUtils.COLOR_ORANGE);
            distanceAxis.setMaxLabelChars(3);
            distanceAxis.setFormatter(new SimpleAxisValueFormatter().setAppendedText(".".toCharArray()));
            distanceAxis.setHasLines(true);
            distanceAxis.setHasTiltedLabels(true);
            data.setAxisXBottom(distanceAxis);

            // Tempo uses minutes so I can't use auto-generated axis because auto-generation works only for decimal
            // system. So generate custom axis values for example every 15 seconds and set custom labels in format
            // minutes:seconds(00:00), you could do it in formatter but here will be faster.
           /* List<AxisValue> axisValues = new ArrayList<AxisValue>();
            for (float i = 0; i < tempoRange; i += 0.25f) {
                // I'am translating float to minutes because I don't have data in minutes, if You store some time data
                // you may skip translation.
                axisValues.add(new AxisValue(i).setLabel(formatMinutes(tempoRange - i)));
            }

            Axis tempoAxis = new Axis(axisValues).setName("height").setHasLines(true).setMaxLabelChars(4)
                    .setTextColor(ChartUtils.COLOR_RED);*/
            data.setAxisYLeft(new Axis().setName("height").setMaxLabelChars(4)
                    .setFormatter(new HeightValueFormatter(scale, sub, 0)));

            // *** Same as in Speed/Height chart.
            // Height axis, this axis need custom formatter that will translate values back to real height values.
            data.setAxisYRight(new Axis().setName("Height [m]").setMaxLabelChars(3)
                    .setFormatter(new HeightValueFormatter(scale, sub, 0)));

            // Set data
            chart.setLineChartData(data);

            // Important: adjust viewport, you could skip this step but in this case it will looks better with custom
            // viewport. Set
            // viewport with Y range 0-12;
            Viewport v = chart.getMaximumViewport();
            v.set(v.left, tempoRange, v.right, 0);
            chart.setMaximumViewport(v);
            chart.setCurrentViewport(v);

        }

        private String formatMinutes(float value) {
            StringBuilder sb = new StringBuilder();

            // translate value to seconds, for example
            int valueInSeconds = (int) (value * 60);
            int minutes = (int) Math.floor(valueInSeconds / 60);
            int seconds = (int) valueInSeconds % 60;

            sb.append(String.valueOf(minutes)).append(':');
            if (seconds < 10) {
                sb.append('0');
            }
            sb.append(String.valueOf(seconds));
            return sb.toString();
        }

        /**
         * Recalculated height values to display on axis. For this example I use auto-generated height axis so I
         * override only formatAutoValue method.
         */
        private static class HeightValueFormatter extends SimpleAxisValueFormatter {

            private float scale;
            private float sub;
            private int decimalDigits;

            public HeightValueFormatter(float scale, float sub, int decimalDigits) {
                this.scale = scale;
                this.sub = sub;
                this.decimalDigits = decimalDigits;
            }

            @Override
            public int formatValueForAutoGeneratedAxis(char[] formattedValue, float value, int autoDecimalDigits) {
                float scaledValue = (value + sub) / scale;
                return super.formatValueForAutoGeneratedAxis(formattedValue, scaledValue, this.decimalDigits);
            }
        }

    }




}