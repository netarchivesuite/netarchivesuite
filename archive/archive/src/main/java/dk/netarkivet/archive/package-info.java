/**
 * This module makes it possible to setup and run a repository with replication, active bit consistency checks for
 * bit-preservation, and support for distributed batch jobs on the archive.
 * <ul>
 *     <li>The archiving component offers a secure environment for storing your harvested material. It is designed for high
 *         preservation guarantees on bit preservation.
 *     </li>
 *     <li>It allows for replication of data on different locations, and distribution of content on several servers on each
 *         location. It supports different software and hardware platforms.
 *     </li>
 *     <li>The module allows for distributed batch jobs, running the same jobs on all servers at a location in parallel,
 *         and merging the results.
 *     </li>
 *     <li>An index of data in the archive allows fast access to the harvested materials.</li>
 * </ul>
 */
package dk.netarkivet.archive;