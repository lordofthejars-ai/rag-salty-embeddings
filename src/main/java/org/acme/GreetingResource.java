package org.acme;

import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Paths;
import org.acme.ai.Ingestor;

@Path("/hello")
public class GreetingResource {

    @Inject
    Ingestor ingestor;

    @Startup
    public void ingestDocuments() throws IOException {

        ingestor.ingest(Paths.get("./rag"));

    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
