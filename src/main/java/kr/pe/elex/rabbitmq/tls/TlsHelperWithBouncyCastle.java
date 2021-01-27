/*
 * Copyright (c) 2021. Elex. All Rights Reserved.
 * https://www.elex-project.com/
 */

package kr.pe.elex.rabbitmq.tls;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final class TlsHelperWithBouncyCastle {
	private TlsHelperWithBouncyCastle(){}

	public static SSLContext context(InputStream clientCrtInputStream, InputStream clientKeyInputStream, char[] clientPassword,
	                                 InputStream caCrtInputStream)
			throws KeyStoreException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException {
		Security.addProvider(new BouncyCastleProvider());

		// CA 인증서 불러오기
		CertificateFactory caCertFactory = CertificateFactory.getInstance("X.509");
		PEMParser parser = new PEMParser(new InputStreamReader(caCrtInputStream));
		X509Certificate caCert = (X509Certificate) caCertFactory
				.generateCertificate(new ByteArrayInputStream(parser.readPemObject().getContent()));
		parser.close();

		// 클라이언트 인증서 불러오기
		CertificateFactory clientCertFactory = CertificateFactory.getInstance("X.509");
		parser = new PEMParser(new InputStreamReader(clientCrtInputStream));
		X509Certificate clientCert = (X509Certificate) clientCertFactory
				.generateCertificate(new ByteArrayInputStream(parser.readPemObject().getContent()));
		parser.close();

		// 클라이언트 비밀키 불러오기
		parser = new PEMParser(new InputStreamReader(clientKeyInputStream));
		Object object = parser.readObject();
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
		KeyPair keyPair;
		if (object instanceof PEMEncryptedKeyPair) {
			// 암호화된 키라면 패스워드가 필요하다.
			PEMEncryptedKeyPair pemKeyPair = (PEMEncryptedKeyPair) object;
			PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(clientPassword);
			keyPair = converter.getKeyPair(pemKeyPair.decryptKeyPair(decProv));
		} else {
			// 암호화되지 않은 키라면 비밀번호가 필요없다.
			PEMKeyPair pemKeyPair = (PEMKeyPair) object;
			keyPair = converter.getKeyPair(pemKeyPair);
		}
		parser.close();
		PrivateKey clientKey = keyPair.getPrivate();

		// CA 인증서를 사용해서 트러스트 스토어를 만든다.
		KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		caKeyStore.load(null, null);
		caKeyStore.setCertificateEntry("ca-certificate", caCert);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(caKeyStore);

		// 클라이언트 인증서와 비밀키를 사용해서 키 스토어를 만든다.
		KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		clientKeyStore.load(null, null);
		clientKeyStore.setCertificateEntry("certificate", clientCert);
		clientKeyStore.setKeyEntry("private-key", clientKey, clientPassword, new java.security.cert.Certificate[]{clientCert});
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(clientKeyStore, clientPassword);

		SSLContext context = SSLContext.getInstance("TLSv1.3");
		context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		return context;
	}

	/**
	 * BouncyCastle을 이용.
	 *
	 * @param caCrtInputStream ca cert pem
	 * @return
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public static SSLContext context(InputStream caCrtInputStream)
			throws KeyStoreException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException {
		Security.addProvider(new BouncyCastleProvider());

		// CA 인증서 불러오기
		CertificateFactory caCertFactory = CertificateFactory.getInstance("X.509");
		PEMParser parser = new PEMParser(new InputStreamReader(caCrtInputStream));
		X509Certificate caCert = (X509Certificate) caCertFactory
				.generateCertificate(new ByteArrayInputStream(parser.readPemObject().getContent()));
		parser.close();

		// CA 인증서를 사용해서 트러스트 스토어를 만든다.
		KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		caKeyStore.load(null, null);
		caKeyStore.setCertificateEntry("ca-certificate", caCert);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(caKeyStore);

		// 클라이언트 인증서와 비밀키는 없으므로, 그냥 만든다.
		KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		clientKeyStore.load(null, null);
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(clientKeyStore, null);

		SSLContext context = SSLContext.getInstance("TLSv1.3");
		context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		return context;
	}

}
