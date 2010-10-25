/* File:       $Id: StartedJobHistoryChartGen.java 1612 2010-10-04 16:05:47Z ngiraud $
 * Revision:   $Revision: 1612 $
 * Author:     $Author: ngiraud $
 * Date:       $Date: 2010-10-04 18:05:47 +0200 (Mon, 04 Oct 2010) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.harvesting.monitor;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.NumberUtils;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;

/**
 * This class implements a generator for an history chart of a running job.
 * The chart traces the progress percentage and the queued URI count over
 * the crawl time.
 * Charts are rendered in a PNG image file, generated in the webapp directory.
 */
class StartedJobHistoryChartGen {

    /**
     * Time units used to scale the crawl time values and generate the
     * chart's time axis ticks.
     */
    private static enum TimeAxisResolution {
        /**
         * One second. Tick step is 10s.
         */
        second(1, 1, 10),
        /**
         * One minute. Tick step is 5m.
         */
        minute(60, 60, 5),
        /**
         * One hour. Tick step is 1h.
         */
        hour(60 * minute.seconds, 60 * minute.seconds, 1),
        /**
         * Twelve hours. Tick step is 2h.
         */
        half_day(12 * 60 * minute.seconds, 60 * minute.seconds, 2),
        /**
         * One day. Tick step is 0.5d.
         */
        day(24 * hour.seconds, 24 * hour.seconds, 0.5),
        /**
         * One week. Tick step is 1w.
         */
        week(7 * day.seconds, 7 * day.seconds, 1);

        /**
         * The time unit in seconds.
         */
        private final int seconds;

        /**
         * The scale in seconds.
         */
        private final int scaleSeconds;

        /**
         * The step between two tick units.
         */
        private final double tickStep;

        /**
         * Builds a time axis resolution.
         * @param seconds the actual resolution in seconds
         * @param scaleSeconds the actual "scale" of ticks
         * @param tickStep the number of ticks in one step.
         */
        TimeAxisResolution(int seconds, int scaleSeconds, double tickStep) {
            this.seconds = seconds;
            this.scaleSeconds = scaleSeconds;
            this.tickStep = tickStep;
        }

        double[] scale(double[] timeInSeconds) {
            double[] scaledTime = new double[timeInSeconds.length];
            for (int i = 0; i < timeInSeconds.length; i++) {
                scaledTime[i] = timeInSeconds[i] / this.scaleSeconds;
            }
            return scaledTime;
        }

        static TimeAxisResolution findTimeUnit(double seconds) {

            TimeAxisResolution[] allTus = values();
            for (int i = 0; i < allTus.length; i++) {
                TimeAxisResolution nextGreater = allTus[i + 1];
                if (seconds < nextGreater.seconds) {
                    return allTus[i];
                }
            }
            return week; // largest unit
        }
    }

    /**
     * Executor class used to schedule chart generation.
     * @see ScheduledThreadPoolExecutor
     */
    private static class ChartGenExecutor
    extends ScheduledThreadPoolExecutor {

        ChartGenExecutor() {
            // We need only 1 thread
            super(1);
        }

        @Override
        protected void afterExecute(Runnable task, Throwable t) {
            super.afterExecute(task, t);
            if (t != null) {
                LOG.error("Error history chart generation", t);
            }
        }

    }

    /**
     * A chart generation task. Generates a PNG image for a
     * job progress history.
     */
    private static class ChartGen implements Runnable {

        private final StartedJobHistoryChartGen gen;

        ChartGen(StartedJobHistoryChartGen gen) {
            super();
            this.gen = gen;
        }

        @Override
        public void run() {

            synchronized (gen) {
                gen.chartFile = null;
            }

            long jobId = gen.jobId;

            StartedJobInfo[] fullHistory =
                RunningJobsInfoDAO.getInstance().getFullJobHistory(jobId);

            LinkedList<Double> timeValues = new LinkedList<Double>();
            LinkedList<Double> progressValues = new LinkedList<Double>();
            LinkedList<Double> urlValues = new LinkedList<Double>();

            for (StartedJobInfo sji : fullHistory) {
                timeValues.add((double) sji.getElapsedSeconds());
                progressValues.add(sji.getProgress());
                urlValues.add((double) sji.getQueuedFilesCount());
            }

            // Refresh the history png image for the job.
            File pngFile = new File(
                    gen.outputFolder,
                    jobId + "-history.png");

            File newPngFile;
            try {
                newPngFile = File.createTempFile(
                        jobId + "-history",
                        "." + System.currentTimeMillis() + ".png");
            } catch (IOException e) {
                LOG.warn("Failed to create temp PNG file for job " + jobId);
                return;
            }

            long startTime = System.currentTimeMillis();
            gen.generatePngChart(
                    newPngFile,
                    CHART_RESOLUTION[0], CHART_RESOLUTION[1],
                    null, // no chart title
                    I18N.getString(
                            gen.locale,
                            "running.job.details.chart.legend.crawlTime"),
                    new String[] {
                        I18N.getString(
                                gen.locale,
                                "running.job.details.chart.legend.progress"),
                                I18N.getString(
                                        gen.locale,
                               "running.job.details.chart.legend.queuedUris") },
                    NumberUtils.toPrimitiveArray(timeValues),
                    new double[][]  {
                        new double[] { 0, 100 },
                        null
                    },
                    new double[][] {
                        NumberUtils.toPrimitiveArray(progressValues),
                        NumberUtils.toPrimitiveArray(urlValues)
                    },
                    new Color[] { Color.blue, Color.green.darker() },
                    new String[] { "%", "" },
                    false,
                    Color.lightGray.brighter().brighter());

            long genTime = System.currentTimeMillis() - startTime;
            LOG.info("Generated history chart for job " + jobId
                    + " in " + (genTime < 1000 ? genTime + " ms" :
                        StringUtils.formatDuration(genTime / 1000))
                        + ".");

            synchronized (gen) {
                // Overwrite old file, then delete temp file
                try {
                    FileUtils.copyFile(newPngFile, pngFile);
                    FileUtils.remove(newPngFile);
                } catch (IOFailure iof) {
                    LOG.error("IOFailure while copying PNG file", iof);
                }
                gen.chartFile = pngFile;
            }

        }

    }

    /** The class logger. */
    final static Log LOG = LogFactory.getLog(
            StartedJobHistoryChartGen.class);

    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);

    /**
     * Rate in seconds at which history charts should be generated.
     */
    private static final long GEN_INTERVAL =
        Settings.getLong(
                HarvesterSettings.HARVEST_MONITOR_HISTORY_CHART_GEN_INTERVAL);

    /**
     * The chart image resolution.
     */
    private static final int[] CHART_RESOLUTION = new int[] { 600, 450 };

    private static final double CHART_AXIS_DIMENSION = 10.0;

    private static final String OUTPUT_REL_PATH  =
        "History" + File.separator + "webapp";

    /** The job id. */
    private final long jobId;

    /**
     * The folder where image files are output.
     */
    private final File outputFolder;

    /**
     * The chart image file.
     */
    private File chartFile = null;

    /**
     * The locale for internationalizing the chart.
     */
    private Locale locale = Locale.getDefault();

    private ChartGenExecutor genExec = null;

    StartedJobHistoryChartGen(long jobId) {
        super();

        this.outputFolder = new File(
                FileUtils.getTempDir() + File.separator
                + OUTPUT_REL_PATH);

        this.jobId = jobId;

        genExec = new ChartGenExecutor();
        genExec.scheduleWithFixedDelay(
                new ChartGen(this),
                0,
                GEN_INTERVAL,
                TimeUnit.SECONDS);
    }

    /**
     * Returns the image file.
     * @return the image file. Might return null if no file is currently
     * available.
     */
    public synchronized File getChartFile() {
        return chartFile;
    }

    /**
     * Deletes the chart image if it exists and stops the generation schedule.
     */
    public void cleanup() {

        if (chartFile != null && chartFile.exists()) {
            if (! chartFile.delete()) {
                chartFile.deleteOnExit();
            }
        }

        if (genExec != null) {
            genExec.shutdownNow();
        }
    }

    /**
     * Generates a chart in PNG format.
     * @param outputFile the output file, it should exist.
     * @param pxWidth the image width in pixels.
     * @param pxHeight the image height in pixels.
     * @param chartTitle the chart title, may be null.
     * @param xAxisTitle the x axis title
     * @param yDataSeriesRange the axis range (null for auto)
     * @param yDataSeriesTitles the Y axis titles.
     * @param timeValuesInSeconds the time values in seconds
     * @param yDataSeries the Y axis value series.
     * @param yDataSeriesColors the Y axis value series drawing colors.
     * @param drawBorder draw, or not, the border.
     * @param backgroundColor the chart background color.
     */
    final void generatePngChart(
            File outputFile,
            int pxWidth, int pxHeight,
            String chartTitle,
            String xAxisTitle,
            String[] yDataSeriesTitles,
            double[] timeValuesInSeconds,
            double[][] yDataSeriesRange,
            double[][] yDataSeries,
            Color[] yDataSeriesColors,
            String[] yDataSeriesTickSuffix,
            boolean drawBorder,
            Color backgroundColor) {

        // Domain axis
        NumberAxis xAxis = new NumberAxis(xAxisTitle);
        xAxis.setFixedDimension(CHART_AXIS_DIMENSION);
        xAxis.setLabelPaint(Color.black);
        xAxis.setTickLabelPaint(Color.black);

        double maxSeconds = getMaxValue(timeValuesInSeconds);
        TimeAxisResolution xAxisRes =
            TimeAxisResolution.findTimeUnit(maxSeconds);
        xAxis.setTickUnit(new NumberTickUnit(xAxisRes.tickStep));
        double[] scaledTimeValues = xAxisRes.scale(timeValuesInSeconds);

        String tickSymbol = I18N.getString(
                locale,
                "running.job.details.chart.timeunit.symbol." + xAxisRes.name());
        xAxis.setNumberFormatOverride(
                new DecimalFormat("###.##'" + tickSymbol + "'"));

        // First dataset
        String firstDataSetTitle = yDataSeriesTitles[0];
        XYDataset firstDataSet = createXYDataSet(
                firstDataSetTitle, scaledTimeValues, yDataSeries[0]);
        Color firstDataSetColor = yDataSeriesColors[0];

        // First range axis
        NumberAxis firstYAxis = new NumberAxis(firstDataSetTitle);

        firstYAxis.setFixedDimension(CHART_AXIS_DIMENSION);
        setAxisRange(firstYAxis, yDataSeriesRange[0]);
        firstYAxis.setLabelPaint(firstDataSetColor);
        firstYAxis.setTickLabelPaint(firstDataSetColor);
        String firstAxisTickSuffix = yDataSeriesTickSuffix[0];
        if (firstAxisTickSuffix != null && !firstAxisTickSuffix.isEmpty()) {
            firstYAxis.setNumberFormatOverride(
                    new DecimalFormat("###.##'" + firstAxisTickSuffix + "'"));
        }

        // Create the plot with domain axis and first range axis
        XYPlot plot = new XYPlot(firstDataSet, xAxis, firstYAxis, null);

        XYLineAndShapeRenderer firstRenderer =
            new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(firstRenderer);

        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        firstRenderer.setSeriesPaint(0, firstDataSetColor);

        // Now iterate on next axes
        for (int i = 1; i < yDataSeries.length; i++) {
            // Create axis
            String seriesTitle = yDataSeriesTitles[i];
            Color seriesColor = yDataSeriesColors[i];
            NumberAxis yAxis = new NumberAxis(seriesTitle);;

            yAxis.setFixedDimension(CHART_AXIS_DIMENSION);
            setAxisRange(yAxis, yDataSeriesRange[i]);

            yAxis.setLabelPaint(seriesColor);
            yAxis.setTickLabelPaint(seriesColor);

            String yAxisTickSuffix = yDataSeriesTickSuffix[i];
            if (yAxisTickSuffix != null && !yAxisTickSuffix.isEmpty()) {
                yAxis.setNumberFormatOverride(
                        new DecimalFormat("###.##'" + yAxisTickSuffix + "'"));
            }

            // Create dataset and add axis to plot
            plot.setRangeAxis(i, yAxis);
            plot.setRangeAxisLocation(i, AxisLocation.BOTTOM_OR_LEFT);
            plot.setDataset(i,
                    createXYDataSet(
                            seriesTitle, scaledTimeValues, yDataSeries[i]));
            plot.mapDatasetToRangeAxis(i, i);
            XYItemRenderer renderer = new StandardXYItemRenderer();
            renderer.setSeriesPaint(0, seriesColor);
            plot.setRenderer(i, renderer);
        }

        // Create the chart
        JFreeChart chart = new JFreeChart(
                chartTitle,
                JFreeChart.DEFAULT_TITLE_FONT,
                plot,
                false);

        // Customize rendering
        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(true);
        chart.setBorderPaint(Color.BLACK);

        // Render image
        try {
            ChartUtilities.saveChartAsPNG(outputFile, chart, pxWidth, pxHeight);
        } catch (IOException e) {
            LOG.error("Chart export failed", e);
        }
    }

    private final XYDataset createXYDataSet(
            String name,
            double[] timeValues,
            double[] values) {

        DefaultXYDataset ds = new DefaultXYDataset();
        ds.addSeries(name, new double[][] { timeValues, values });

        return ds;
    }

    private final double getMaxValue(double[] values) {
        double max = Double.MIN_VALUE;
        for (double v : values) {
            max = Math.max(v, max);
        }
        return max;
    }

    /**
     * Sets the locale.
     * @param locale the locale used for chart text.
     */
    void setLocale(Locale locale) {
        this.locale = locale;
    }

    private void setAxisRange(NumberAxis axis, double[] range) {
        if (range == null || range.length != 2) {
            axis.setAutoRange(true);
        } else {
            double lower = range[0];
            double upper = range[1];
            ArgumentNotValid.checkTrue(lower < upper, "Incorrect range");
            axis.setAutoRange(false);
            axis.setRange(new Range(lower, upper));
        }
    }

}
