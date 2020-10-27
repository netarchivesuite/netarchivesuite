/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.harvester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

public class HarvesterSettingsTester {

    @Test
    public void testNoFinalSettingsConstants() {
        Class<HarvesterSettings> c = HarvesterSettings.class;
        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {
            // Check that all static public fields are not final
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                assertFalse(
                        "public static fields must not be final, " + "but this was violated by field " + f.getName(),
                        Modifier.isFinal(modifiers));
            }
        }
    }

    /**
     * If this test fails, we need to update the SingleMBeanObjectTester#Setup and ChannelIDTester.
     */
    @Test
    public void testHarvestControllerPrioritySettingUnchanged() {
        assertEquals("The 'HarvesterSettings.HARVEST_CONTROLLER_CHANNEL' " + "setting has changed. Please update "
                + "SingleMBeanObjectTester#Setup method", HarvesterSettings.HARVEST_CONTROLLER_CHANNEL,
                "settings.harvester.harvesting.channel");
    }
}
