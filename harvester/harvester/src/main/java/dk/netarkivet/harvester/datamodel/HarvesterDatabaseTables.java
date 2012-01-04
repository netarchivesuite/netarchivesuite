/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.datamodel;

/**
 * Constants definining the tables of the Harvester database and the 
 * required versions of the individual tables.  
 */
public class HarvesterDatabaseTables {

    public static final String DOMAINS_TABLE = "domains";
    public static final int DOMAINS_TABLE_REQUIRED_VERSION = 2;
    public static final String CONFIGURATIONS_TABLE = "configurations";
    public static final int CONFIGURATIONS_TABLE_REQUIRED_VERSION = 5;

    public static final String SEEDLISTS_TABLE = "seedlists";
    public static final int SEEDLISTS_TABLE_REQUIRED_VERSION = 1;

    public static final String PASSWORDS_TABLE = "passwords";
    public static final int PASSWORDS_TABLE_REQUIRED_VERSION = 1;

    public static final String OWNERINFO_TABLE = "ownerinfo";
    public static final int OWNERINFO_TABLE_REQUIRED_VERSION = 1;

    public static final String HISTORYINFO_TABLE = "historyinfo";
    public static final int HISTORYINFO_TABLE_REQUIRED_VERSION = 2;

    public static final String CONFIGPASSWORDS_TABLE = "config_passwords";
    public static final int CONFIGPASSWORDS_TABLE_REQUIRED_VERSION = 1;

    public static final String CONFIGSEEDLISTS_TABLE = "config_seedlists";
    public static final int CONFIGSEEDLISTS_TABLE_REQUIRED_VERSION = 1;

    public static final String HARVESTDEFINITIONS_TABLE = "harvestdefinitions";
    public static final int HARVESTDEFINITIONS_TABLE_REQUIRED_VERSION = 2;

    public static final String PARTIALHARVESTS_TABLE = "partialharvests";
    public static final int PARTIALHARVESTS_TABLE_REQUIRED_VERSION = 1;

    public static final String FULLHARVESTS_TABLE = "fullharvests";
    public static final int FULLHARVESTS_TABLE_REQUIRED_VERSION = 5;

    public static final String HARVESTCONFIGS_TABLE = "harvest_configs";
    public static final int HARVESTCONFIGS_TABLE_REQUIRED_VERSION = 1;

    public static final String SCHEDULES_TABLE = "schedules";
    public static final int SCHEDULES_TABLE_REQUIRED_VERSION = 1;

    public static final String ORDERTEMPLATES_TABLE = "ordertemplates"; 
    public static final int ORDERTEMPLATES_TABLE_REQUIRED_VERSION = 1;

    public static final String JOBS_TABLE = "jobs";
    public static final int JOBS_TABLE_REQUIRED_VERSION = 7;

    public static final String JOBCONFIGS_TABLE = "job_configs";
    public static final int JOBCONFIGS_TABLE_REQUIRED_VERSION = 1;

    public static final String GLOBALCRAWLERTRAPLISTS_TABLE = "global_crawler_trap_lists";
    public static final int GLOBALCRAWLERTRAPLISTS_TABLE_REQUIRED_VERSION = 1;

    public static final String GLOBALCRAWLERTRAPEXPRESSIONS_TABLE 
    = "global_crawler_trap_expressions";
    public static final int GLOBALCRAWLERTRAPEXPRESSIONS_TABLE_REQUIRED_VERSION = 1;

    public static final String RUNNINGJOBSHISTORY_TABLE = "runningjobshistory";
    public static final int RUNNINGJOBSHISTORY_TABLE_REQUIRED_VERSION = 2;

    public static final String RUNNINGJOBSMONITOR_TABLE = "runningjobsmonitor";
    public static final int RUNNINGJOBSMONITOR_TABLE_REQUIRED_VERSION = 2;

    public static final String FRONTIERREPORTMONITOR_TABLE = "frontierreportmonitor"; 
    public static final int FRONTIERREPORTMONITOR_TABLE_REQUIRED_VERSION = 1;

    public static final String EXTENDEDFIELD_TABLE = "extendedfield"; 
    public static final int EXTENDEDFIELD_TABLE_REQUIRED_VERSION = 1;

    public static final String EXTENDEDFIELDVALUE_TABLE = "extendedfieldvalue"; 
    public static final int EXTENDEDFIELDVALUE_TABLE_REQUIRED_VERSION = 1;

    public static final String EXTENDEDFIELDTYPE_TABLE = "extendedfieldtype"; 
    public static final int EXTENDEDFIELDTYPE_TABLE_REQUIRED_VERSION = 1;
    
}
