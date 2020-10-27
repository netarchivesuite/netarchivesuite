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
package dk.netarkivet.harvester.datamodel;

import java.sql.Connection;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.DBUtils;

/**
 * Enum class defining the tables of the Harvester database and the required versions of the individual tables.
 */
public enum HarvesterDatabaseTables {

    /** The table containing information about domains. */
    DOMAINS {
        static final String NAME = "domains";
        static final int REQUIRED_VERSION = 3;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about domain-configurations. */
    CONFIGURATIONS {
        static final String NAME = "configurations";
        static final int REQUIRED_VERSION = 5;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about seedlists. */
    SEEDLISTS {
        static final String NAME = "seedlists";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /**
     * The table containing information about passwords. Currently not used.
     */
    PASSWORDS {
        static final String NAME = "passwords";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /**
     * The table containing information about ownerinfo. Currently not used.
     */
    OWNERINFO {
        static final String NAME = "ownerinfo";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about history info. */
    HISTORYINFO {
        static final String NAME = "historyinfo";
        static final int REQUIRED_VERSION = 2;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /**
     * The table containing information about config passwords. Currently not used.
     */
    CONFIGPASSWORDS {
        static final String NAME = "config_passwords";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about config seedlists. */
    CONFIGSEEDLISTS {
        static final String NAME = "config_seedlists";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about harvestdefinitions. */
    HARVESTDEFINITIONS {
        static final String NAME = "harvestdefinitions";
        static final int REQUIRED_VERSION = 4;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about partial harvests. */
    PARTIALHARVESTS {
        static final String NAME = "partialharvests";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about full harvests. */
    FULLHARVESTS {
        static final String NAME = "fullharvests";
        static final int REQUIRED_VERSION = 5;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about harvest configs */
    HARVESTCONFIGS {
        static final String NAME = "harvest_configs";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about harvest schedules. */
    SCHEDULES {
        static final String NAME = "schedules";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about Heritrix templates. */
    ORDERTEMPLATES {
        static final String NAME = "ordertemplates";
        static final int REQUIRED_VERSION = 2;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about jobs. */
    JOBS {
        static final String NAME = "jobs";
        static final int REQUIRED_VERSION = 10;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about jobconfigs. */
    JOBCONFIGS {
        static final String NAME = "job_configs";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about list of global crawlertraps. */
    GLOBALCRAWLERTRAPLISTS {
        static final String NAME = "global_crawler_trap_lists";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about global crawlertrap expressions. */
    GLOBALCRAWLERTRAPEXPRESSIONS {
        static final String NAME = "global_crawler_trap_expressions";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about running jobs history. */
    RUNNINGJOBSHISTORY {
        static final String NAME = "runningjobshistory";
        static final int REQUIRED_VERSION = 2;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about running jobs monitor. */
    RUNNINGJOBSMONITOR {
        static final String NAME = "runningjobsmonitor";
        static final int REQUIRED_VERSION = 2;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about frontier report monitor. */
    FRONTIERREPORTMONITOR {
        static final String NAME = "frontierreportmonitor";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about extended fields. */
    EXTENDEDFIELD {
        static final String NAME = "extendedfield";
        static final int REQUIRED_VERSION = 2;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about extended field values. */
    EXTENDEDFIELDVALUE {
        static final String NAME = "extendedfieldvalue";
        static final int REQUIRED_VERSION = 2;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** The table containing information about extended field types. */
    EXTENDEDFIELDTYPE {
        static final String NAME = "extendedfieldtype";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** Harvest channels. */
    HARVESTCHANNELS {
        static final String NAME = "harvestchannel";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** Harvest channels. */
    EAVTYPEATTRIBUTE {
        static final String NAME = "eav_type_attribute";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    },
    /** Harvest channels. */
    EAVATTRIBUTE {
        static final String NAME = "eav_attribute";
        static final int REQUIRED_VERSION = 1;

        @Override
        public int getRequiredVersion() {
            return REQUIRED_VERSION;
        }

        @Override
        public String getTablename() {
            return NAME;
        }
    };

    /** @return required version of table. */
    public abstract int getRequiredVersion();

    /** @return name of database table. */
    public abstract String getTablename();

    /**
     * Check that a database table has the required version.
     * <p>
     * NB: the provided connection is not closed.
     *
     * @param connection connection to the database.
     * @param table The table to check up against required version
     * @throws IllegalState if the version isn't as required.
     */
    public static void checkVersion(Connection connection, HarvesterDatabaseTables table) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNull(table, "HarvesterDatabaseTables table");

        int actualVersion = DBUtils.getTableVersion(connection, table.getTablename());
        if (actualVersion != table.getRequiredVersion()) {
            String message = "Wrong table version for '" + table.getTablename() + "': " + "Should be "
                    + table.getRequiredVersion() + ", but is " + actualVersion;
            throw new IllegalState(message);
        }
    }

}
