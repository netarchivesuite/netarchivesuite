/*
 * #%L
 * Netarchivesuite - monitor - test
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
package dk.netarkivet.monitor.webinterface;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

/**
 * Unittests for class JMXSummaryUtils.
 */
public class JMXSummaryUtilsTester {

    @Test
    public void testGenerateMessage() {
        String input = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam volutpat euismod aliquet. Nullam vestibulum mollis arcu, quis laoreet nibh aliquet et. In at ligula pellentesque magna placerat luctus. Donec mauris nibh, lacinia non feugiat quis, dapibus id orci. Suspendisse sollicitudin suscipit sodales. Mauris interdum consectetur nunc sed interdum. Nulla facilisi. Quisque urna lectus, tempor ut feugiat sit amet, congue eget lectus. Duis eget interdum turpis. Morbi turpis arcu, venenatis ac venenatis nec, pretium ac tellus. Fusce condimentum iaculis sem. Cras eros dui, imperdiet vitae faucibus feugiat, pellentesque eu quam. In dignissim facilisis sollicitudin. Cras tincidunt arcu at lectus tincidunt a porta lorem accumsan. Pellentesque porta, est at viverra sagittis, est elit congue lorem, feugiat lobortis tellus nisl in augue.";
        String output = JMXSummaryUtils.generateMessage(input, Locale.getDefault());
        // System.out.println(output);
        assertNotNull(output);
        assertTrue("Should have split String into many lines, not " + output.split("\n").length,
                output.split("\n").length > 10);
    }

    @Test
    public void testGenerateMessageWithForcing() {
        String input = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam volutpat euismod aliquet. Nullam vestibulum mollis arcu, quis laoreet nibh aliquet et. In at ligula aaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbcccccccccccccccccdddddddddddddddddddddeeeeeeeeeeeeeeepellentesque magna placerat luctus. Donec mauris nibh, lacinia non feugiat quis, dapibus id orci. Suspendisse sollicitudin suscipit sodales. Mauris interdum consectetur nunc sed interdum. Nulla facilisi. Quisque urna lectus, tempor ut feugiat sit amet, congue eget lectus. Duis eget interdum turpis. Morbi turpis arcu, venenatis ac venenatis nec, pretium ac tellus. Fusce condimentum iaculis sem. Cras eros dui, imperdiet vitae faucibus feugiat, pellentesque eu quam. In dignissim facilisis sollicitudin. Cras tincidunt arcu at lectus tincidunt a porta lorem accumsan. Pellentesque porta, est at viverra sagittis, est elit congue lorem, feugiat lobortis tellus nisl in augue.";
        String output = JMXSummaryUtils.generateMessage(input, Locale.getDefault());
        // System.out.println(output);
        assertNotNull(output);
        assertTrue("Should have split String into many lines, not " + output.split("\n").length,
                output.split("\n").length > 10);
    }
}
