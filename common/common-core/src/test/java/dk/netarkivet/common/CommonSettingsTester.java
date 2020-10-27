/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;

import org.junit.Test;

/**
 * Test that all the public static fields for the CommonSettings class are <i>not</i> final.
 */
public class CommonSettingsTester {

    @Test
    public void testNoFinalSettingsConstants() {
        Class<CommonSettings> c = CommonSettings.class;
        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {
            int modifiers = f.getModifiers();
            if (isPublic(modifiers) && isStatic(modifiers)) {
                assertFalse("CommonSettings: field final: " + f.getName(), isFinal(modifiers));
            }
        }
    }
}
