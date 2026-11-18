package dk.netarkivet.restapi.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/harvests")
@Produces(MediaType.APPLICATION_JSON)
public class HarvestResource {

    @GET
    public List<String> getHarvests() {
        return List.of("example-harvest");
    }

    @GET
    @Path("/{id}")
    public String getHarvest(@PathParam("id") String id) {
        return "Harvest " + id;
    }
}