package dk.netarkivet.restapi.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/status")
public class StatusResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Status getStatus() {
        return new Status("NetArchiveSuite", "OK");
    }

    public record Status(String application, String status) {}
}