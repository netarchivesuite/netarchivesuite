package dk.netarkivet.common.api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path( "/hello/{name}" )
/**
 * Created by csr on 9/4/17.
 */
public class Hello {
    @GET
       public String getThing( @PathParam( "name" ) String name )
       {
           return "Hello, " + name;
       }

}
