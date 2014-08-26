/*
 * #%L
 * NetarchiveSuite System test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.systemtest;

/**
 * Encapsulates the attributes found for an application
 * dk.netarkivet.systemtestoverview page.
 */
public class Application {
    private final String machine;

    private final String application;

    private final String instance_Id;

    private final String priority;

    private final String replica;

    /**
     * Maps empty strings to null.
     * 
     * @param machine
     * @param application
     * @param instance_Id
     * @param priority
     * @param replica
     */
    public Application(String machine, String application, String instance_Id, String priority, String replica) {
        this.machine = "".equals(machine) ? null : machine;
        this.application = "".equals(application) ? null : application;
        this.instance_Id = "".equals(instance_Id) ? null : instance_Id;
        this.priority = "".equals(priority) ? null : priority;
        this.replica = "".equals(replica) ? null : replica;
    }

    @Override
    public String toString() {
        return "Application [machine=" + machine + ", application=" + application + ", instance_Id=" + instance_Id
                + ", priority=" + priority + ", replica=" + replica + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((application == null) ? 0 : application.hashCode());
        result = prime * result + ((instance_Id == null) ? 0 : instance_Id.hashCode());
        result = prime * result + ((machine == null) ? 0 : machine.hashCode());
        result = prime * result + ((priority == null) ? 0 : priority.hashCode());
        result = prime * result + ((replica == null) ? 0 : replica.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Application other = (Application) obj;
        if (application == null) {
            if (other.application != null)
                return false;
        } else if (!application.equals(other.application))
            return false;
        if (instance_Id == null) {
            if (other.instance_Id != null)
                return false;
        } else if (!instance_Id.equals(other.instance_Id))
            return false;
        if (machine == null) {
            if (other.machine != null)
                return false;
        } else if (!machine.equals(other.machine))
            return false;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        if (replica == null) {
            if (other.replica != null)
                return false;
        } else if (!replica.equals(other.replica))
            return false;
        return true;
    }
}
