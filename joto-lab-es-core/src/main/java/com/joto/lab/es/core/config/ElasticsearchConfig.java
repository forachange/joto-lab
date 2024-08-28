package com.joto.lab.es.core.config;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.joto.lab.es.core.utils.EncryptUtil;
import com.joto.lab.es.core.utils.EncryptProcessor;
import com.joto.lab.es.core.utils.EsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * @author joey
 * @date 2022/5/18 15:01
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "elasticsearch", name = "enable", havingValue = "true")
public class ElasticsearchConfig {

    /**
     * 逗号
     */
    public static final String COMMA = ",";
    /**
     * 冒号
     */
    public static final String COLON = ":";
    private static String staUsername;
    private static String staPassword;
    private static String staHosts;
    private static String staScheme;
    private static String staCaFile;
    private static String staSecret;
    private static String staIv;
    private String username;
    private String password;
    private String hosts;
    private String scheme;
    private String caFile;
    private String secret;
    private String iv;

    public static String getUsername() {
        return EncryptProcessor.unwrapEncryptedValue(staUsername);
    }

    @Value("${elasticsearch.username}")
    public void setUsername(String username) {
        this.username = username;
    }

    public static String getPassword() {
        return EncryptProcessor.unwrapEncryptedValue(staPassword);
    }

    @Value("${elasticsearch.password}")
    public void setPassword(String password) {
        this.password = password;
    }

    public static String getHosts() {
        return staHosts;
    }

    @Value("${elasticsearch.hosts}")
    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public static String getScheme() {
        return staScheme;
    }

    @Value("${elasticsearch.scheme}")
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public static String getCaFile() {
        return staCaFile;
    }

    @Value("${elasticsearch.caFile}")
    public void setCaFile(String caFile) {
        this.caFile = caFile;
    }

    public static String getSecret() {
        return EncryptProcessor.unwrapEncryptedValue(staSecret);
    }

    @Value("${elasticsearch.secret}")
    public void setSecret(String secret) {
        this.secret = secret;
    }

    public static String getIv() {
        return EncryptProcessor.unwrapEncryptedValue(staIv);
    }

    @Value("${elasticsearch.iv}")
    public void setIv(String iv) {
        this.iv = iv;
    }

    @PostConstruct
    public synchronized void init() {
        setValues(username, password, hosts, caFile, scheme, secret, iv);
    }

    private static void setValues(final String username, final String password, final String hosts, final String caFile,
                                  final String scheme, final String secret, final String iv) {
        staUsername = username;
        staPassword = password;
        staHosts = hosts;
        staCaFile = caFile;
        staScheme = scheme;
        staSecret = secret;
        staIv = iv;
    }


    @Bean
    public ElasticsearchClient elasticsearchClient() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, KeyManagementException {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        String oriUser = EncryptUtil.decrypt4(getUsername(), getSecret(), getIv());
        String oriPwd = EncryptUtil.decrypt4(getPassword(), getSecret(), getIv());
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(oriUser, oriPwd));

        // 分割成地址数组
        String[] hostArr = hosts.split(COMMA);
        HttpHost[] httpHosts = new HttpHost[hostArr.length];
        int i = 0;
        /* 添加 */
        for (String host : hostArr) {
            String[] hostPort = host.split(COLON);
            httpHosts[i++] = new HttpHost(hostPort[0], Integer.parseInt(hostPort[1]), scheme);

        }

        Certificate trustedCa;
        CertificateFactory factory =
                CertificateFactory.getInstance("X.509");

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final Resource[] resources = resolver.getResources(caFile);
        final Resource resource = resources[0];
        try (InputStream is = resource.getInputStream()) {
            trustedCa = factory.generateCertificate(is);
        }

        KeyStore trustStore = KeyStore.getInstance("pkcs12");
        trustStore.load(null);
        trustStore.setCertificateEntry("ca", trustedCa);

        SSLContextBuilder sslBuilder = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null);

        final SSLContext sslContext = sslBuilder.build();

        RestClientBuilder builder = RestClient.builder(httpHosts);
        builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslContext));

        RestClient restClient = builder.build();

        ElasticsearchTransport transport = new RestClientTransport(restClient, EsUtil.getMapper());

        return new ElasticsearchClient(transport);
    }
}
