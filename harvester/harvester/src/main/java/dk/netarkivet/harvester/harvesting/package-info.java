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
package dk.netarkivet.harvester.harvesting;