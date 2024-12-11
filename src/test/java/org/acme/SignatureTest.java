package org.acme;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Base64;

import static java.nio.file.Files.readAllBytes;

public class SignatureTest {


    @Test
    public void verify() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnrecoverableKeyException {

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream("keystore.jks"), "changeit".toCharArray());

        PrivateKey privateKey = (PrivateKey) keyStore.getKey("signerKeyPair", "changeit".toCharArray());

        Signature signatureSign = Signature.getInstance("SHA256withRSA");
        signatureSign.initSign(privateKey);

        byte[] messageBytes = readAllBytes(Paths.get("rag/faq.txt"));
        signatureSign.update(messageBytes);
        byte[] digitalSignature = signatureSign.sign();
        String sign = Base64.getEncoder().encodeToString(digitalSignature);

        Certificate certificate = keyStore.getCertificate("signerKeyPair");
        PublicKey pk = certificate.getPublicKey();

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(pk);

        String content = sign;
        byte[] signatureBytes = Base64.getDecoder().decode(content);
        signature.update(messageBytes);
        boolean result = signature.verify(signatureBytes);


        System.out.println(result);
    }

}
