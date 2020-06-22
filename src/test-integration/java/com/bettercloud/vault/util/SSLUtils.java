package com.bettercloud.vault.util;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/**
 * Static utility methods for generating client-side SSL certs and keys, for tests that use Vault's TLS Certificate
 * auth backend.  Right now, all such code is isolated to {@link com.bettercloud.vault.api.AuthBackendCertTests}.
 */
public class SSLUtils implements TestConstants {

    private SSLUtils() {
    }

    /**
     * <p>Constructs a Java truststore in JKS format, containing the Vault server certificate, so that Vault clients
     * configured with this JKS will trust that certificate.</p>
     *
     * <p>Also constructs a JKS keystore, with a client certificate to use for authentication with Vault's TLS
     * Certificate auth backend.  Stores this cert as a PEM file as well, so that can be registered with Vault
     * as a recognized certificate in {@link VaultContainer#setupBackendCert(String)}.</p>
     *
     * <p>This method must be called AFTER {@link VaultContainer#initAndUnsealVault()}, and BEFORE
     * {@link VaultContainer#setupBackendCert(String)}.</p>
     *
     * @throws IOException When certificate was not created
     * @return
     */
    public static HashMap<String, Object> createClientCertAndKey() throws IOException {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        final X509CertificateHolder certificateHolder = getX509CertificateHolder();
        final X509Certificate vaultCertificate = getCertificate(certificateHolder);

        KeyStore clientTrustStore = getClientTrustStore(vaultCertificate);

        // Store the Vault's server certificate as a trusted cert in the truststore

        // Generate a client certificate, and store it in a Java keystore
        final KeyPair keyPair = generateKeyPair();
        final X509Certificate clientCertificate = generateCert(keyPair);
        if (clientCertificate == null) {
            throw new IOException("Failed to generate certificate");
        }
        final KeyStore clientKeystore = getClientKeystore(keyPair, clientCertificate);

        // Also write the client certificate to a PEM file, so it can be registered with Vault
        String certToPem = certToPem(clientCertificate);
        String privateKeyToPem = privateKeyToPem(keyPair.getPrivate());
        return new HashMap<String, Object>() {
            {
                put("clientKeystore", clientKeystore);
                put("clientTrustStore", clientTrustStore);
                put("cert", certToPem);
                put("privateKey", privateKeyToPem);
            }
        };
    }

    private static KeyStore getClientTrustStore(X509Certificate vaultCertificate) throws IOException {
        final KeyStore trustStore = emptyStore();
        try {
            trustStore.setCertificateEntry("cert", vaultCertificate);
        } catch (KeyStoreException e) {
            throw new IOException("Cannot create trust keystore.", e);
        }
        return trustStore;
    }

    private static KeyStore getClientKeystore(KeyPair keyPair, X509Certificate clientCertificate) {
        try {
            final KeyStore keyStore = emptyStore();
            keyStore.setKeyEntry("privatekey", keyPair.getPrivate(), PASSWORD.toCharArray(), new Certificate[]{clientCertificate});
            keyStore.setCertificateEntry("cert", clientCertificate);
            return keyStore;
        } catch (KeyStoreException | IOException e) {
            return null;
        }
    }

    private static X509CertificateHolder getX509CertificateHolder() {
        final PEMParser pemParser;
        try (FileReader fileReader = new FileReader(CERT_PEMFILE)) {
            pemParser = new PEMParser(fileReader);
            return (X509CertificateHolder) pemParser.readObject();
        } catch (IOException e) {
            return null;
        }
    }

    private static X509Certificate getCertificate(X509CertificateHolder certificateHolder) {
        try {
            return new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(certificateHolder);
        } catch (CertificateException e) {
            return null;
        }
    }

    /**
     * See https://www.cryptoworkshop.com/guide/, chapter 3
     *
     * @return A 4096-bit RSA key pair
     */
    private static KeyPair generateKeyPair() throws IOException {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
            keyPairGenerator.initialize(4096);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            if (keyPair == null) {
                throw new IOException("Failed to generate keypair");
            }
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to generate keypair", e);
        }
    }

    /**
     * See http://www.programcreek.com/java-api-examples/index.php?api=org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
     *
     * @param keyPair The RSA keypair with which to generate the certificate
     * @return An X509 certificate
     */
    private static X509Certificate generateCert(final KeyPair keyPair) {
        String issuer = "C=AU, O=The Legion of the Bouncy Castle, OU=Client Certificate, CN=localhost";
        final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                new X500Name(issuer),
                BigInteger.ONE,
                new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30),
                new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
                new X500Name(issuer),
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        final GeneralNames subjectAltNames = new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
        try {
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        } catch (CertIOException e) {
            e.printStackTrace();
            return null;
        }

        final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WithRSAEncryption");
        final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        final BcContentSignerBuilder signerBuilder = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
        final X509CertificateHolder x509CertificateHolder;
        try {
            final AsymmetricKeyParameter keyp = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
            final ContentSigner signer = signerBuilder.build(keyp);
            x509CertificateHolder = certificateBuilder.build(signer);
        } catch (IOException | OperatorCreationException e) {
            e.printStackTrace();
            return null;
        }

        final X509Certificate certificate;
        try {
            certificate = new JcaX509CertificateConverter().getCertificate(x509CertificateHolder);
            certificate.checkValidity(new Date());
            certificate.verify(keyPair.getPublic());
        } catch (CertificateException | SignatureException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        }

        return certificate;
    }

    /**
     * See https://stackoverflow.com/questions/3313020/write-x509-certificate-into-pem-formatted-string-in-java
     *
     * @param certificate An X509 certificate
     * @return String certificate  in pem format
     */
    private static String certToPem(final X509Certificate certificate) throws IOException {
        final Base64.Encoder encoder = Base64.getMimeEncoder();

        final String certHeader = "-----BEGIN CERTIFICATE-----\n";
        final String certFooter = "\n-----END CERTIFICATE-----";
        final byte[] certBytes;
        try {
            certBytes = certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new IOException("Failed to encode certificate", e);
        }
        final String certContents = new String(encoder.encode(certBytes));
        return certHeader + certContents + certFooter;
    }

    /**
     * See https://stackoverflow.com/questions/3313020/write-x509-certificate-into-pem-formatted-string-in-java
     *
     * @param key      An RSA private key
     * @return String private key in pem format
     */
    private static String privateKeyToPem(final PrivateKey key) {
        final Base64.Encoder encoder = Base64.getMimeEncoder();

        final String keyHeader = "-----BEGIN PRIVATE KEY-----\n";
        final String keyFooter = "\n-----END PRIVATE KEY-----";
        final byte[] keyBytes = key.getEncoded();
        final String keyContents = new String(encoder.encode(keyBytes));
        return keyHeader + keyContents + keyFooter;
    }

    /**
     * @param CN Common Name, is X.509 speak for the name that distinguishes
     *           the Certificate best, and ties it to your Organization
     * @param OU Organizational unit
     * @param O  Organization NAME
     * @param L  Location
     * @param S  State
     * @param C  Country
     * @return
     * @throws Exception
     */
    public static String generatePKCS10(KeyPair kp, String CN, String OU, String O,
                                        String L, String S, String C) throws IOException, OperatorCreationException {
        X500Principal subject = new X500Principal(String.format("C=%s, ST=%s, L=%s, O=%s, OU=%s, CN=%S", C, S, L, O, OU, CN));
        ContentSigner signGen = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, kp.getPublic());
        PKCS10CertificationRequest csr = builder.build(signGen);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (Writer osWriter = new OutputStreamWriter(output)) {
                try (JcaPEMWriter pem = new JcaPEMWriter(osWriter)) {
                    pem.writeObject(csr);
                }
            }
            return new String(output.toByteArray());
        }
    }

    public static KeyStore emptyStore() throws IOException {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");

            // Loading creates the store, can't do anything with it until it's loaded
            ks.load(null, PASSWORD.toCharArray());
            return ks;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new IOException("Cannot create empty keystore.", e);
        }
    }
}
