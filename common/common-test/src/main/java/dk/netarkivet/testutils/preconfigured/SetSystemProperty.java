/*
 * #%L
 * Netarchivesuite - common - test
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

package dk.netarkivet.testutils.preconfigured;

/**
 * This class allows setting a system property temporarily. Do not attempt to set it to null, as that will break.
 */
public class SetSystemProperty implements TestConfigurationIF {
    private String oldValue;
    private String property;
    private String newValue;

    public SetSystemProperty(String property, String newValue) {
        this.property = property;
        this.newValue = newValue;
    }

    public void setUp() {
        oldValue = System.getProperty(property);
        System.setProperty(property, newValue);
    }

    public void tearDown() {
        if (oldValue != null) {
            System.setProperty(property, oldValue);
        } else {
            System.clearProperty(property);
        }
    }
}
