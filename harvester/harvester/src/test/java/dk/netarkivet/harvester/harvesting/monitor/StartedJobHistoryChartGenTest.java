/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.NumberUtils;
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
        gen.setLocale(Locale.getDefault());
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

}
