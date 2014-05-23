/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.NumberUtils;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobHistoryChartGen.TimeAxisResolution;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class StartedJobHistoryChartGenTest extends TestCase {

    ReloadSettings rs = new ReloadSettings();

    StartedJobHistoryChartGen gen;

    @Override
    protected void setUp() throws Exception {
        rs.setUp();
        super.setUp();

        Settings.set(
                CommonSettings.DIR_COMMONTEMPDIR,
                TestInfo.WORKING_DIR.getPath());

        gen = new StartedJobHistoryChartGen(1);
    }

    public void tearDown() throws Exception {

        FileUtils.removeRecursively(TestInfo.WORKING_DIR);

        super.tearDown();
        rs.tearDown();

        gen.cleanup();
    }

    public final void testPngGeneration() throws IOException, ParseException {

        File testDataDir = new File(TestInfo.BASEDIR, "charting");
        File testData = new File(testDataDir, "job266.csv");

        List<Double> timeValues = new LinkedList<Double>();
        List<Double> progressValues = new LinkedList<Double>();
        List<Double> urlValues = new LinkedList<Double>();
        List<Double> randomValues = new LinkedList<Double>();

        BufferedReader in = new BufferedReader(new FileReader(testData));
        String line = in.readLine(); // skip header line
        while ((line = in.readLine()) != null) {
            String[] parts = line.split(";");

            timeValues.add(
                    Math.floor(Double.parseDouble(parts[0])));
            progressValues.add(
                    Math.floor(Double.parseDouble(parts[1])));
            urlValues.add(Double.parseDouble(parts[2]));
            randomValues.add(Math.random());
        }
        in.close();

        File pngFile = new File(TestInfo.WORKING_DIR, "266-history.png");
        TestInfo.WORKING_DIR.mkdirs();
        pngFile.createNewFile();

        gen.generatePngChart(
                pngFile,
                512, 384,
                "Test history graph",
                "Crawl time",
                new String[] { "Progress", "URL", "Random" },
                NumberUtils.toPrimitiveArray(timeValues),
                new double[][] {
                    new double[] { 0, 100 },
                    null,
                    null
                },
                new double[][] {
                    NumberUtils.toPrimitiveArray(progressValues),
                    NumberUtils.toPrimitiveArray(urlValues),
                    NumberUtils.toPrimitiveArray(randomValues)
                },
                new Color[] {
                    Color.red, Color.blue, Color.green },
                    new String[] { "%", "", "" },
                true,
                Color.lightGray.brighter().brighter());

    }

    public final void testPngGenerationWithNoData()
    throws IOException, ParseException {

        File pngFile = new File(TestInfo.WORKING_DIR, "empty-history.png");
        TestInfo.WORKING_DIR.mkdirs();
        pngFile.createNewFile();

        gen.generatePngChart(
                pngFile,
                512, 384,
                null,
                "",
                new String[] { "%", ""},
                new double[0],
                new double[][] {
                        new double[] { 0, 100 },
                        null
                },
                new double[][] {
                        new double[0],
                        new double[0]
                },
                new Color[] {
                    Color.blue, Color.green.darker()},
                new String[] { "", "" },
                true,
                Color.lightGray.brighter().brighter());

    }

    /**
     * Tests {@link TimeAxisResolution#findTimeUnit(double)}
     */
    public final void testFindTimeUnit() {

        long[] durations = new long[] {
                2,             // 2s
                2*60,          // 2min
                2*60*60,       // 2h
                14*60*60,      // 16h
                2*24*3600,     // 2d
                2*7*24*3600,   // 2w
                60*24*3600,    // 60d
                100*7*24*3600  // 100w
        };

        TimeAxisResolution[] expected = new TimeAxisResolution[] {
                TimeAxisResolution.second,
                TimeAxisResolution.minute,
                TimeAxisResolution.hour,
                TimeAxisResolution.half_day,
                TimeAxisResolution.day,
                TimeAxisResolution.week,
                TimeAxisResolution.week,
                TimeAxisResolution.week
        };

        for (int i = 0; i < durations.length; i++) {
            assertEquals(
                    expected[i], TimeAxisResolution.findTimeUnit(durations[i]));
        }
    }

}
