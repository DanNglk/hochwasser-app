package de.bitdroid.flooding.levels;

import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_HTHW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_HTHW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MHW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MHW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MNW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MNW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MTHW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MTHW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MTNW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MTNW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_NTNW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_NTNW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_TIMESTAMP;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_TYPE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_ZERO_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_ZERO_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_STATION_KM;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_STATION_NAME;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_WATER_NAME;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeriesFormatter;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.pegelonline.PegelOnlineSource;
import de.bitdroid.flooding.utils.AbstractLoaderCallbacks;
import de.bitdroid.flooding.utils.Log;

public class GraphActivity extends Activity implements OnTouchListener {
	
	public static final String EXTRA_WATER_NAME = "waterName";
	private static final int LOADER_ID = 44;

	private XYPlot graph;
	private PointF zoomMinXY, zoomMaxXY, graphMinXY, graphMaxXY;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.graph);

		final String waterName = getIntent().getExtras().getString(EXTRA_WATER_NAME);
		graph = (XYPlot) findViewById(R.id.graph);
		final SeriesManager manager = new SeriesManager();

		// regular water level (relative values)
		manager.addSeries(
				new SimpleSeries(
					"Water levels",
					COLUMN_STATION_KM, 
					COLUMN_LEVEL_VALUE, 
					COLUMN_LEVEL_UNIT),
				getDefaultFormatter(R.xml.series_water_levels));

		// add characteristic values
		manager.addSeries(
				new SimpleSeries(
					"MW",
					COLUMN_STATION_KM,
					COLUMN_CHARVALUES_MW_VALUE,
					COLUMN_CHARVALUES_MW_UNIT),
				getDefaultFormatter(R.xml.series_average_levels));
		manager.addSeries(
				new SimpleSeries(
					"MHW",
					COLUMN_STATION_KM,
					COLUMN_CHARVALUES_MHW_VALUE,
					COLUMN_CHARVALUES_MHW_UNIT),
				getDefaultFormatter(R.xml.series_average_levels));
		manager.addSeries(
				new SimpleSeries(
					"MNW",
					COLUMN_STATION_KM,
					COLUMN_CHARVALUES_MNW_VALUE,
					COLUMN_CHARVALUES_MNW_UNIT),
				getDefaultFormatter(R.xml.series_average_levels));

		// add tild series
		manager.addSeries(
				new SimpleSeries(
					"MTHW",
					COLUMN_STATION_KM,
					COLUMN_CHARVALUES_MTHW_VALUE,
					COLUMN_CHARVALUES_MTHW_UNIT),
				getDefaultFormatter(R.xml.series_average_tide_values));
		manager.addSeries(
				new SimpleSeries(
					"MTNW",
					COLUMN_STATION_KM,
					COLUMN_CHARVALUES_MTNW_VALUE,
					COLUMN_CHARVALUES_MTNW_UNIT),
				getDefaultFormatter(R.xml.series_average_tide_values));
		manager.addSeries(
				new SimpleSeries(
					"HTHW",
					COLUMN_STATION_KM,
					COLUMN_CHARVALUES_HTHW_VALUE,
					COLUMN_CHARVALUES_HTHW_UNIT),
				getDefaultFormatter(R.xml.series_extreme_tide_values));
		manager.addSeries(
				new SimpleSeries(
					"NTNW",
					COLUMN_STATION_KM,
					COLUMN_CHARVALUES_NTNW_VALUE,
					COLUMN_CHARVALUES_NTNW_UNIT),
				getDefaultFormatter(R.xml.series_extreme_tide_values));



		for (Pair<AbstractSeries, XYSeriesFormatter<?>> p : manager.getSeries()) {
			graph.addSeries(p.first, p.second);
		}

		graph.setOnTouchListener(this);
		graph.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
		graph.getLayoutManager().remove(graph.getLegendWidget());
		graph.setTitle(waterName);
		graph.setTicksPerRangeLabel(2);
		graph.setTicksPerDomainLabel(3);
		graph.getGraphWidget().setRangeValueFormat(new DecimalFormat("@@##"));
		graph.getGraphWidget().setDomainValueFormat(new DecimalFormat("@@#"));
		graph.setBorderStyle(Plot.BorderStyle.NONE, null, null);

		graph.setBackgroundColor(Color.BLACK);
		graph.getGraphWidget().getBackgroundPaint().setColor(Color.BLACK);
		graph.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);

		AbstractLoaderCallbacks loader = new AbstractLoaderCallbacks(LOADER_ID) {

			@Override
			protected void onLoadFinishedHelper(Loader<Cursor> loader, Cursor cursor) {
				if (cursor == null) return;

				manager.reset();
				manager.addData(cursor);
				graph.redraw();

				graph.calculateMinMaxVals();
				graphMinXY = new PointF(
						graph.getCalculatedMinX().floatValue(),
						graph.getCalculatedMinY().floatValue());
				graphMaxXY = new PointF(
						graph.getCalculatedMaxX().floatValue(), 
						graph.getCalculatedMaxY().floatValue());
				zoomMinXY = new PointF(graphMinXY.x, graphMinXY.y);
				zoomMaxXY = new PointF(graphMaxXY.x, graphMaxXY.y);

				graph.setRangeBoundaries(graphMinXY.y, graphMaxXY.y, BoundaryMode.FIXED);

			}

			@Override
			protected void onLoaderResetHelper(Loader<Cursor> loader) { }

			@Override
			protected Loader<Cursor> getCursorLoader() {
				return new CursorLoader(
						getApplicationContext(),
						new PegelOnlineSource().toUri(),
						new String[] { 
							COLUMN_STATION_NAME,
							COLUMN_STATION_KM,
							COLUMN_LEVEL_TIMESTAMP,
							COLUMN_LEVEL_VALUE,
							COLUMN_LEVEL_UNIT,
							COLUMN_LEVEL_ZERO_VALUE,
							COLUMN_LEVEL_ZERO_UNIT,
							COLUMN_CHARVALUES_MW_VALUE,
							COLUMN_CHARVALUES_MW_UNIT,
							COLUMN_CHARVALUES_MHW_VALUE,
							COLUMN_CHARVALUES_MHW_UNIT,
							COLUMN_CHARVALUES_MNW_VALUE,
							COLUMN_CHARVALUES_MNW_UNIT,
							COLUMN_CHARVALUES_MTHW_VALUE,
							COLUMN_CHARVALUES_MTHW_UNIT,
							COLUMN_CHARVALUES_MTNW_VALUE,
							COLUMN_CHARVALUES_MTNW_UNIT,
							COLUMN_CHARVALUES_HTHW_VALUE,
							COLUMN_CHARVALUES_HTHW_UNIT,
							COLUMN_CHARVALUES_NTNW_VALUE,
							COLUMN_CHARVALUES_NTNW_UNIT
						}, COLUMN_WATER_NAME + "=? AND " + COLUMN_LEVEL_TYPE + "=?", 
						new String[] { waterName, "W" }, 
						null);
			}
		};

		getLoaderManager().initLoader(LOADER_ID, null, loader);
    }




	private LineAndPointFormatter getDefaultFormatter(int configuration) {
		LineAndPointFormatter formatter = new LineAndPointFormatter();
		formatter.setPointLabelFormatter(null);
		formatter.configure(getApplicationContext(), configuration);
		return formatter;
	}



    static final int NONE = 0;
    static final int ONE_FINGER_DRAG = 1;
    static final int TWO_FINGERS_DRAG = 2;
    int mode = NONE;

    private PointF firstFinger;
    private float distBetweenFingers;
	private float scrollingPan;
	private SmoothScrollRunnable scrollRunnable;


    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
				cancelScrolling();
                firstFinger = new PointF(event.getX(), event.getY());
                mode = ONE_FINGER_DRAG;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
				if (mode == ONE_FINGER_DRAG) {
					scrollRunnable = new SmoothScrollRunnable(scrollingPan);
					new Thread(scrollRunnable).start();
				}
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // second finger
				cancelScrolling();
                distBetweenFingers = spacing(event);
                // the distance check is done to avoid false alarms
                if (distBetweenFingers > 5f) {
                    mode = TWO_FINGERS_DRAG;
				}
                break;
            case MotionEvent.ACTION_MOVE:
				cancelScrolling();
                if (mode == ONE_FINGER_DRAG) {
                    PointF oldFirstFinger = firstFinger;
                    firstFinger = new PointF(event.getX(), event.getY());
					scrollingPan = oldFirstFinger.x - firstFinger.x;
					scroll(scrollingPan);
                    graph.setDomainBoundaries(zoomMinXY.x, zoomMaxXY.x, BoundaryMode.FIXED);
                    graph.redraw();

                } else if (mode == TWO_FINGERS_DRAG) {
                    float oldDist = distBetweenFingers;
                    distBetweenFingers = spacing(event);
                    zoom(oldDist / distBetweenFingers);
                    graph.setDomainBoundaries(zoomMinXY.x, zoomMaxXY.x, BoundaryMode.FIXED);
                    graph.redraw();
                }
                break;
        }
        return true;
    }



    private void zoom(float scale) {
        float domainSpan = zoomMaxXY.x - zoomMinXY.x;
        float domainMidPoint = zoomMaxXY.x - domainSpan / 2.0f;
        float offset = domainSpan * scale / 2.0f;

        zoomMinXY.x = domainMidPoint - offset;
        zoomMaxXY.x = domainMidPoint + offset;

        clampToDomainBounds(false, domainSpan);
    }

    private void scroll(float pan) {
        float domainSpan = zoomMaxXY.x - zoomMinXY.x;
        float step = domainSpan / graph.getWidth();
        float offset = pan * step;
        zoomMinXY.x = zoomMinXY.x + offset;
        zoomMaxXY.x = zoomMaxXY.x + offset;
        clampToDomainBounds(true, domainSpan);
    }

    private void clampToDomainBounds(boolean scroll, float domainSpan) {
		float leftBoundary = graphMinXY.x;
		float rightBoundary = graphMaxXY.x;
        if (zoomMinXY.x < leftBoundary) {
            zoomMinXY.x = leftBoundary;
            if (scroll) zoomMaxXY.x = leftBoundary + domainSpan;
		}
		if (zoomMaxXY.x > rightBoundary) {
            zoomMaxXY.x = rightBoundary;
			if (scroll) zoomMinXY.x = rightBoundary - domainSpan;
        }
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

	private void cancelScrolling() {
		if (scrollRunnable != null) scrollRunnable.stopScrolling();
		scrollRunnable = null;
	}


	private class SmoothScrollRunnable implements Runnable {
		
		private static final long REFRESH_RATE = 5;

		private boolean stopped = false;
		private float pan;

		public SmoothScrollRunnable(float pan) {
			this.pan = pan;
		}

		@Override
		public void run() {
			do {
				if (stopped) return;
				pan *= 0.97;
				scroll(pan);
				graph.setDomainBoundaries(zoomMinXY.x, zoomMaxXY.x, BoundaryMode.FIXED);
				graph.redraw();
				try {
					Thread.sleep(REFRESH_RATE);
				} catch (InterruptedException ie) {
					Log.warning("Thread was interrupted");
				}
			} while (Math.abs(pan) > 0.3);
		}

		public synchronized void stopScrolling() {
			stopped = true;
		}
	}
}