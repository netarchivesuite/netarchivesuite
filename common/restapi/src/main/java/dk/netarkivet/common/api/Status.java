package dk.netarkivet.common.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import dk.netarkivet.monitor.jmx.HostForwarding;
import dk.netarkivet.monitor.logging.SingleLogRecord;
import dk.netarkivet.monitor.registry.distribute.MonitorRegistryServer;
import dk.netarkivet.monitor.webinterface.JMXStatusEntry;
import dk.netarkivet.monitor.webinterface.StatusEntry;

/**
 * Created by csr on 9/5/17.
 */
@Path("/status")
public class Status {

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public JMXStatusEntry[] getAllStatus() {
        String query = "dk.netarkivet.common.logging:*,index=0";
        ArrayList<JMXStatusEntry> entries = new ArrayList<>();
/*        MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
        Set<ObjectName> resultSet = null;
        try {

            HostForwarding.getInstance(SingleLogRecord.class, mBeanServer, query);
            resultSet = mBeanServer.queryNames(new ObjectName(query), null);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }*/
        List<StatusEntry> entries1 = null;
        try {
            entries1 = JMXStatusEntry.queryJMX(query);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
        for (StatusEntry entry: entries1) {
            entry.getLogMessage(Locale.getDefault());
            entries.add((JMXStatusEntry) entry);
        }
        return entries.toArray(new JMXStatusEntry[]{});
    }


}
