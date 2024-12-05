package org.acme.ai;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkiverse.langchain4j.chroma.ChromaEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class Ingestor {

    @Inject
    ChromaEmbeddingStore store;

    @Inject
    EmbeddingModel embeddingModel;

    public void ingest(Path directory) throws IOException {
        List<Document> files;
        try (Stream<Path> streamFiles = Files.list(directory)) {
            final List<Document> documents = streamFiles
                .map(f -> loadDocument(f, new TextDocumentParser()))
                .toList();

            files = new ArrayList<>(documents);
        }

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .documentSplitter(recursive(500, 0))
            .build();

        ingestor.ingest(files);
    }
}
