package org.acme;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Singleton
public class KeyProducer {

    @Inject
    Logger logger;


    @Produces
    @Singleton
    public KeyStore loadKeyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        logger.info("Loading keystore.jks keystore");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream("keystore.jks"), "changeit".toCharArray());

        return keyStore;
    }

    @Named("signer")
    @Produces
    Signature sign(PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException {

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);

        return signature;

    }

    @Named("verifier")
    @Produces
    Signature verify(PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException {

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);

        return signature;
    }

    @Produces
    PrivateKey getPrivateKey(KeyStore keyStore) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return (PrivateKey) keyStore.getKey("signerKeyPair", "changeit".toCharArray());
    }

    @Produces
    PublicKey getPublicKey(KeyStore keyStore) throws KeyStoreException {
        Certificate certificate = keyStore.getCertificate("signerKeyPair");
        return certificate.getPublicKey();
    }

}
