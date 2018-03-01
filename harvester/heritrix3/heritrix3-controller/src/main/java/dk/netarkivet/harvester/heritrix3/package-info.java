/*
 * #%L
 * Netarchivesuite - harvester
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
/**
 * This module handles defining, scheduling, and execution of harvests.
 * <ul>
 *     <li>Harvesting uses the Heritrix crawler developed by Internet Archive. The harvesting module allows for flexible
 *         automated definitions of harvests. The system gives access to the full power of the Heritrix crawler, given
 *        adequate knowledge of the Heritrix crawler. NetarchiveSuite wraps the crawler in an easy-to-use interface that
 *         handles scheduling and configuring of the crawls, and distributes it to several crawling servers.
 *     </li>
 *     <li>The harvester module allows for de-duplication, using an index of URLs already crawled and stored in the archive
 *         to avoid storing duplicates more than once. This function uses the de-duplicator module from Kristinn
 *         Sigurdsson.
 *     </li>
 *     <li>The harvester module supports packaging metadata about the harvest together with the harvested data.
 *     </li>
 * </ul>
 */
package dk.netarkivet.harvester.heritrix3;

