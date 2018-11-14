/*
 * #%L
 * Netarchivesuite - deploy
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.deploy;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestTemplate {

    @Test
    public void test_template() {
        String[] strArray;
        Map<String, String> env = new HashMap<String, String>();
        String str;
        String separator;
        String expected;
        boolean bFailOnMissing;
        boolean bExceptionExpected;
        String expectedMessage;
        Object[][] testCases;

        Template tpl = new Template();
        Assert.assertNotNull(tpl);

        testCases = new Object[][] { {new String[] {}, null, ""}, {new String[] {""}, null, ""},
                {new String[] {"Netarkivet.dk", "NetarchiveSuite"}, null, "Netarkivet.dkNetarchiveSuite"},
                {new String[] {"Netarkivet.dk", "NetarchiveSuite"}, "", "Netarkivet.dkNetarchiveSuite"},
                {new String[] {"Netarkivet.dk", "NetarchiveSuite"}, "\r\n", "Netarkivet.dk\r\nNetarchiveSuite\r\n"}};

        for (int i = 0; i < testCases.length; ++i) {
            strArray = (String[]) testCases[i][0];
            separator = (String) testCases[i][1];
            expected = (String) testCases[i][2];
            str = Template.untemplate(strArray, env, false, separator);
            Assert.assertEquals(expected, str);
        }

        env.put("NAS", "NetarchiveSuite");
        env.put("nas", "netarkivet.dk");

        testCases = new Object[][] { {"${NAS}", "NetarchiveSuite", true, false, null},
                {"100$", "100$", true, false, null}, {"NA$A", "NA$A", true, false, null},
                {"NA$$A", "NA$A", true, false, null}, {"Welcome to the ${", "Welcome to the ${", true, false, null},
                {"${${NAS}}", "}", false, false, null},
                {"${${NAS}}", "}", true, true, "Env is missing replacement for: ${NAS"},
                {"$${NAS}", "${NAS}", true, false, null},
                {"Find ${NAS} at ${nas}", "Find NetarchiveSuite at netarkivet.dk", true, false, null},
                {"Find ${NAS} at ${nAs}", "Find NetarchiveSuite at ", false, false, null},
                {"Find ${NaS} at ${nas}", "Find  at netarkivet.dk", true, true, "Env is missing replacement for: NaS"}};

        for (int i = 0; i < testCases.length; ++i) {
            str = (String) testCases[i][0];
            expected = (String) testCases[i][1];
            bFailOnMissing = (Boolean) testCases[i][2];
            bExceptionExpected = (Boolean) testCases[i][3];
            expectedMessage = (String) testCases[i][4];
            try {
                str = Template.untemplate(str, env, bFailOnMissing);
                Assert.assertEquals(expected, str);
                Assert.assertFalse(bExceptionExpected);
            } catch (Throwable t) {
                if (t instanceof IllegalArgumentException) {
                    Assert.assertTrue(bExceptionExpected);
                    Assert.assertEquals(expectedMessage, t.getMessage());
                } else {
                    t.printStackTrace();
                    Assert.fail("Unexpected exception type!");
                }
            }
        }
    }

}
