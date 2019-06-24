package com.bettercloud.vault.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
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

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
     * as a recognized certificate in {@link VaultContainer#setupBackendCert()}.</p>
     *
     * <p>This method must be called AFTER {@link VaultContainer#initAndUnsealVault()}, and BEFORE
     * {@link VaultContainer#setupBackendCert()}.</p>
     *
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static void createClientCertAndKey() throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException,
            OperatorCreationException {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        final FileReader fileReader = new FileReader(CERT_PEMFILE);
        final PEMParser pemParser = new PEMParser(fileReader);
        final X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();
        final X509Certificate vaultCertificate = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateHolder);

        // Store the Vault's server certificate as a trusted cert in the truststore
        final KeyStore trustStore = KeyStore.getInstance("jks");
        trustStore.load(null);
        trustStore.setCertificateEntry("cert", vaultCertificate);
        try (final FileOutputStream keystoreOutputStream = new FileOutputStream(CLIENT_TRUSTSTORE)) {
            trustStore.store(keystoreOutputStream, "password".toCharArray());
        }

        // Generate a client certificate, and store it in a Java keystore
        final KeyPair keyPair = generateKeyPair();
        final X509Certificate clientCertificate =
                generateCert(keyPair, "C=AU, O=The Legion of the Bouncy Castle, OU=Client Certificate, CN=localhost");
        final KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null);
        keyStore.setKeyEntry("privatekey", keyPair.getPrivate(), "password".toCharArray(), new Certificate[]{clientCertificate});
        keyStore.setCertificateEntry("cert", clientCertificate);
        try (final FileOutputStream keystoreOutputStream = new FileOutputStream(CLIENT_KEYSTORE)) {
            keyStore.store(keystoreOutputStream, "password".toCharArray());
        }

        // Also write the client certificate to a PEM file, so it can be registered with Vault
        writeCertToPem(clientCertificate, CLIENT_CERT_PEMFILE);
        writePrivateKeyToPem(keyPair.getPrivate(), CLIENT_PRIVATE_KEY_PEMFILE);
    }

    /**
     * See https://www.cryptoworkshop.com/guide/, chapter 3
     *
     * @return A 4096-bit RSA key pair
     * @throws NoSuchAlgorithmException
     */
    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
        keyPairGenerator.initialize(4096);
        return keyPairGenerator.genKeyPair();
    }

    /**
     * See http://www.programcreek.com/java-api-examples/index.php?api=org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
     *
     * @param keyPair The RSA keypair with which to generate the certificate
     * @param issuer  The issuer (and subject) to use for the certificate
     * @return An X509 certificate
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    private static X509Certificate generateCert(final KeyPair keyPair, final String issuer) throws IOException,
            CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException,
            SignatureException, OperatorCreationException {
        final String subject = issuer;
        final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                new X500Name(issuer),
                BigInteger.ONE,
                new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30),
                new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
                new X500Name(subject),
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        final GeneralNames subjectAltNames = new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
        certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

        final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WithRSAEncryption");
        final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        final BcContentSignerBuilder signerBuilder = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
        final AsymmetricKeyParameter keyp = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        final ContentSigner signer = signerBuilder.build(keyp);
        final X509CertificateHolder x509CertificateHolder = certificateBuilder.build(signer);

        final X509Certificate certificate = new JcaX509CertificateConverter()
                .getCertificate(x509CertificateHolder);
        certificate.checkValidity(new Date());
        certificate.verify(keyPair.getPublic());
        return certificate;
    }

    /**
     * See https://stackoverflow.com/questions/3313020/write-x509-certificate-into-pem-formatted-string-in-java
     *
     * @param certificate An X509 certificate
     * @param filename    The name (including path) of a file to which the certificate will be written in PEM format
     * @throws CertificateEncodingException
     * @throws FileNotFoundException
     */
    private static void writeCertToPem(final X509Certificate certificate, final String filename)
            throws CertificateEncodingException, FileNotFoundException {
        final Base64.Encoder encoder = Base64.getMimeEncoder();

        final String certHeader = "-----BEGIN CERTIFICATE-----\n";
        final String certFooter = "\n-----END CERTIFICATE-----";
        final byte[] certBytes = certificate.getEncoded();
        final String certContents = new String(encoder.encode(certBytes));
        final String certPem = certHeader + certContents + certFooter;
        try (final PrintWriter out = new PrintWriter(filename)) {
            out.println(certPem);
        }
    }

    /**
     * See https://stackoverflow.com/questions/3313020/write-x509-certificate-into-pem-formatted-string-in-java
     *
     * @param key      An RSA private key
     * @param filename The name (including path) of a file to which the private key will be written in PEM format
     * @throws FileNotFoundException
     */
    private static void writePrivateKeyToPem(final PrivateKey key, final String filename) throws FileNotFoundException {
        final Base64.Encoder encoder = Base64.getMimeEncoder();

        final String keyHeader = "-----BEGIN PRIVATE KEY-----\n";
        final String keyFooter = "\n-----END PRIVATE KEY-----";
        final byte[] keyBytes = key.getEncoded();
        final String keyContents = new String(encoder.encode(keyBytes));
        final String keyPem = keyHeader + keyContents + keyFooter;
        try (final PrintWriter out = new PrintWriter(filename)) {
            out.println(keyPem);
        }
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
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Writer osWriter = new OutputStreamWriter(output);
        JcaPEMWriter pem = new JcaPEMWriter(osWriter);
        pem.writeObject(csr);
        pem.close();
        return new String(output.toByteArray());
    }


}
