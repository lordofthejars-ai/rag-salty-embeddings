package org.acme.ai;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkiverse.langchain4j.pgvector.PgVectorEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Decorated;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class Ingestor {

    private final Signature signature;

    @Inject
    PgVectorEmbeddingStore store;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    Logger logger;

    @ConfigProperty(name = "rag.sign.enabled")
    boolean enableSignatureVerifier;

    public Ingestor(@Named("verifier") Signature signature) {
        this.signature = signature;
    }


    public void ingest(Path directory) throws IOException {

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.txt");

        List<Document> files;
        try (Stream<Path> streamFiles = Files.list(directory)) {
            final List<Document> documents = streamFiles
                    // skip signature files and not supported files
                .filter(f -> matcher.matches(f.getFileName()))
                .filter(f -> {
                        if (enableSignatureVerifier) {
                            return validateSignature(f);
                        }
                        // if no signature enabled then all files are valid
                        return true;
                    })
                .map(f -> loadDocument(f, new TextDocumentParser()))
                .toList();

            files = new ArrayList<>(documents);
        }

        logger.infof("Calculating vectors with %s", embeddingModel.getClass());

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .documentSplitter(recursive(500, 0))
            .build();

        ingestor.ingest(files);
    }

    private boolean validateSignature(Path f) {
        Path signatureFile = f.resolveSibling(f.getFileName() + ".sha256");

        if (Files.exists(signatureFile)) {
            try {
                String signature = Files.readString(signatureFile);
                byte[] signatureBytes = Base64.getDecoder().decode(signature);
                this.signature.update(Files.readAllBytes(f));
                boolean result = this.signature.verify(signatureBytes);
                logger.infof("Verifying %s with %s and result: %s", f.getFileName(), signatureFile.getFileName(), result);

                return result;
            } catch (IOException | SignatureException e) {
                e.printStackTrace();
            }

        }

        return false;
    }
}
