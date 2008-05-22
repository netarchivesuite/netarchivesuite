/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.distribute;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.testutils.ReflectUtils;

/**
 * Constants for this package's tests.
 *
 */

public class TestInfo {
    public static Job getJob() throws NoSuchMethodException,
                                      IllegalAccessException,
                                      InvocationTargetException,
                                      InstantiationException {
        Constructor<Job> c = ReflectUtils.getPrivateConstructor(
                Job.class, Long.class, Map.class, JobPriority.class, Long.TYPE,
                Long.TYPE, JobStatus.class, String.class, Document.class,
                String.class, Integer.TYPE);
        return c.newInstance(42L, Collections.<String, String>emptyMap(),
                             JobPriority.LOWPRIORITY, -1L, -1L,
                             JobStatus.STARTED, "default_template",
                             DocumentFactory.getInstance().createDocument(),
                             "", 1);
    }
}
