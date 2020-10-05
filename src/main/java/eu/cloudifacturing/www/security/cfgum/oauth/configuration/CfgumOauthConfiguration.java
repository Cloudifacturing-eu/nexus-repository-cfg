package eu.cloudifacturing.www.security.cfgum.oauth.configuration;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

@Singleton
@Named
public class CfgumOauthConfiguration {
    private static final String CONFIG_FILE = "cfgumoauth.properties";

    private static final String DEFAULT_CFGUM_CERT_URL = "https://api.emgora.eu/v1/emgum/server/auth/realms/cfg/protocol/openid-connect/certs";

    private static final String CFGUM_CERT_URL_KEY = "cfgum.cert.url";

    private static final Duration DEFAULT_PRINCIPAL_CACHE_TTL = Duration.ofMinutes(1);

    private static final String CFGUM_PRINCIPAL_CACHE_TTL_KEY = "cfgum.principal.cache.ttl";

    private static final Logger log = LoggerFactory.getLogger(CfgumOauthConfiguration.class);

    private Properties configuration;

    @PostConstruct
    public void init() {
        configuration = new Properties();

        try {
            configuration.load(Files.newInputStream(Paths.get(".", "etc", CONFIG_FILE)));
        } catch (IOException e) {
            log.warn("Error reading CFGUM oauth properties, falling back to default configuration");
        }
    }

    public String getDefaultCfgumCertUrl(){
        return configuration.getProperty(CFGUM_CERT_URL_KEY, DEFAULT_CFGUM_CERT_URL);
    }

    public Duration getPrincipalCacheTtl() {
        return Duration.parse(configuration.getProperty(CFGUM_PRINCIPAL_CACHE_TTL_KEY, DEFAULT_PRINCIPAL_CACHE_TTL.toString()));
    }
}
