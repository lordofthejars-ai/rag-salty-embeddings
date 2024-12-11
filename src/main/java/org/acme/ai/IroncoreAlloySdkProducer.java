package org.acme.ai;

import com.ironcorelabs.ironcore_alloy_java.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

@ApplicationScoped
public class IroncoreAlloySdkProducer {

    private final byte[] keyByteArray = "hJdwvEeg5mxTu9qWcWrljfKs1ga4MpQ9MzXgLxtlkwX//yA=".getBytes();


    @Produces
    Standalone createStandaloneSdk() throws AlloyException {
        StandardSecrets standardSecrets = new StandardSecrets(10, Arrays.asList(new StandaloneSecret(10, new Secret(keyByteArray))));
        Map<SecretPath, RotatableSecret> deterministicSecrets = Map.of(
                new SecretPath(""),
                new RotatableSecret(
                        new StandaloneSecret(2, new Secret(keyByteArray)),
                        new StandaloneSecret(1, new Secret(keyByteArray))
                )
        );
        StandaloneConfiguration config = getStandaloneConfiguration(standardSecrets, deterministicSecrets);
        return new Standalone(config);
    }

    private @NotNull StandaloneConfiguration getStandaloneConfiguration(StandardSecrets standardSecrets, Map<SecretPath, RotatableSecret> deterministicSecrets) throws AlloyException {
        float approximationFactor = 1.1f;
        Map<SecretPath, VectorSecret> vectorSecrets = Map.of(
                new SecretPath(""),
                new VectorSecret(approximationFactor,
                        new RotatableSecret(
                                new StandaloneSecret(2, new Secret(keyByteArray)),
                                new StandaloneSecret(1, new Secret(keyByteArray))
                        )
                )
        );
        StandaloneConfiguration config = new StandaloneConfiguration(standardSecrets, deterministicSecrets, vectorSecrets);
        return config;
    }

}
