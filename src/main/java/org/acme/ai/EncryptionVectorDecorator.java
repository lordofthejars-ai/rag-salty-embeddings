package org.acme.ai;

import com.ironcorelabs.ironcore_alloy_java.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

// Decorates with encryption

@Decorator
@Priority(10)
public class EncryptionVectorDecorator implements EmbeddingModel {

    @Inject @Delegate @Any
    EmbeddingModel delegate;

    @Inject
    Standalone sdk;

    @Inject
    Logger logger;

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {

        logger.info("Creates vector with encryption");

        Response<List<Embedding>> embeddingsResponse = delegate.embedAll(textSegments);
        List<Embedding> plainEmbeddings = embeddingsResponse.content();

        List<Embedding> encryptedEmbeddings = plainEmbeddings.stream()
                // Get the vector
                .map(Embedding::vector)
                // Transforms to a list, we do in this way because one has Float and the other requires float
                .map(vector -> {
                            List<Float> vectorList = new ArrayList<>();
                            for (float point : vector) {
                                vectorList.add(point);
                            }

                            return vectorList;
                        }
                )
                // Encrypt the vector
                .map(vector -> {

                    PlaintextVector pv = new PlaintextVector(vector,
                            new SecretPath(""),
                            new DerivationPath(""));

                    AlloyMetadata alloyMetadata = AlloyMetadata.newSimple(new TenantId("tenant-1"));
                    try {
                        return sdk.vector().encrypt(pv, alloyMetadata).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                // We get back to LangChain4J embedding
                .map(ev -> {
                    List<Float> encryptedVector = ev.encryptedVector();
                    float[] vector = new float[encryptedVector.size()];
                    for (int i = 0; i < encryptedVector.size(); i++) {
                        vector[i] = encryptedVector.get(i);
                    }

                    return new Embedding(vector);
                })
                .toList();


        return new Response<>(encryptedEmbeddings);
    }
}
