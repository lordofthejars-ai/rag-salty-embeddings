package org.acme;

import com.pgvector.PGvector;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Base64;

import jakarta.ws.rs.core.Response;
import org.acme.ai.Ingestor;
import org.jboss.logging.Logger;

import javax.sql.DataSource;

import static java.nio.file.Files.readAllBytes;

@Path("/")
public class RagResource {

    private final Signature signature;

    public RagResource(@Named("signer") Signature signature) {
        this.signature = signature;
    }

    private DecimalFormat decimalFormat = new DecimalFormat("#.###");

    @Inject
    Ingestor ingestor;

    @Inject
    DataSource dataSource;

    @Inject
    PrivateKey privateKey;

    @Inject
    PublicKey publicKey;

    private static final String RAG_DIR = "./rag";

    @Startup
    public void ingestDocuments() throws IOException {

        ingestor.ingest(Paths.get(RAG_DIR));
        printAllDbContent();
    }

    private void printAllDbContent() {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from public.embeddings;")) {

            try (ResultSet rs = ps.executeQuery()) {
                printResults(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void printResults(ResultSet rs) throws SQLException {
        while(rs.next()) {
            PGvector pGvector = (PGvector) rs.getObject("embedding");
            final float[] vectors = pGvector.toArray();
            final float[] printingVectors = Arrays.copyOfRange(vectors, 0, 5);

            String text = rs.getString("text");

            String metadata = rs.getString("metadata");

            System.out.println("*******************************************************************************************");
            System.out.println("+ " + printFloat(printingVectors));
            System.out.println("-------------------------------------------------------------------------------------------");
            System.out.println("+ " + text);
            System.out.println("-------------------------------------------------------------------------------------------");
            System.out.println("+ " + metadata);
            System.out.println("*******************************************************************************************");

        }
    }

    @Inject
    Logger logger;

    @Inject
    EmbeddingModel embeddingModel;

    @GET
    @Path("distance")
    public void measureDistance() throws SQLException {


        String USER_QUERY = "How do I verify my account?";
        dev.langchain4j.model.output.Response<Embedding> embed = embeddingModel.embed(USER_QUERY);
        PGvector pgVector = new PGvector(embed.content().vector());

        String sqlQuery = "SELECT * FROM public.embeddings ORDER BY embedding <-> cast(? as vector) LIMIT 1";

        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sqlQuery)) {
            ps.setObject(1, pgVector);
            try (ResultSet rs = ps.executeQuery()) {
                printResults(rs);
            }
        }
    }

    @GET
    @Path("sign")
    public Response sign() throws IOException {

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.txt");

        Files.list(Paths.get(RAG_DIR))
                .filter(f -> matcher.matches(f.getFileName()))
                .forEach(f -> {
                    try {

                        logger.infof("Signing %s file", f.toAbsolutePath().toString());

                        byte[] messageBytes = readAllBytes(f.toAbsolutePath());
                        signature.update(messageBytes);
                        byte[] digitalSignature = signature.sign();
                        String sign = Base64.getEncoder().encodeToString(digitalSignature);

                        Files.writeString(f.resolveSibling(f.getFileName() + ".sha256"), sign);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });


        return Response.ok().build();
    }

    private String printFloat(float[] vector) {
        StringBuilder sb = new StringBuilder("[ ");
        for(float v : vector) {
            sb.append(decimalFormat.format(v));
            sb.append("... , ");
        }

        sb.append("]");

        return sb.toString();
    }
}
