package eu.cloudifacturing.www.security.cfgum.oauth.api;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import eu.cloudifacturing.www.security.cfgum.oauth.CfgumAuthenticationException;
import eu.cloudifacturing.www.security.cfgum.oauth.CfgumPrincipal;
import eu.cloudifacturing.www.security.cfgum.oauth.configuration.CfgumOauthConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
@Named("CfgumApiClient")
public class CfgumApiClient {
    private static final Logger log = LoggerFactory.getLogger(CfgumApiClient.class);
    private CfgumOauthConfiguration configuration;
    private Cache<String, CfgumPrincipal> tokenToPrincipalCache;

    @Inject
    public CfgumApiClient(CfgumOauthConfiguration configuration) {
        this.configuration = configuration;
    }

    @PostConstruct
    public void init() {
        tokenToPrincipalCache = CacheBuilder.newBuilder()
                .expireAfterWrite(configuration.getPrincipalCacheTtl().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    public CfgumPrincipal authz(String login, char[] token) throws CfgumAuthenticationException {
        // Combine the login and the token as the cache key since they are both used to generate the principal. If either changes we should obtain a new
        // principal.
        String cacheKey = login + "|" + new String(token);
        CfgumPrincipal cached = tokenToPrincipalCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Using cached principal for login: {}", login);
            return cached;
        } else {
            CfgumPrincipal principal = doAuthz(login, token);
            tokenToPrincipalCache.put(cacheKey, principal);
            return principal;
        }
    }

    private CfgumPrincipal doAuthz(String loginName, char[] token) throws CfgumAuthenticationException {
        CfgumPrincipal authPrincipal = new CfgumPrincipal();
        Set<String> roles = new HashSet<>();
        try {
            String kId = JWT.decode(new String(token)).getKeyId();	// Decode token to obtain the key identifier.
            JwkProvider provider = new UrlJwkProvider(new URL(configuration.getDefaultCfgumCertUrl()));
            Jwk jwk = provider.get(kId);
            RSAPublicKey rpk = (RSAPublicKey) jwk.getPublicKey();
            Algorithm algorithm = Algorithm.RSA256(rpk, null); // the private key is null in this case.
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(new String(token));	// This operation generates a JWTVerificationException, if token is invalid.
            authPrincipal.setUsername(jwt.getClaims().get("preferred_username").asString());
            if(jwt.getClaim("resource_access").asMap().toString().contains("developer")){
                roles.add("developer");
            }
            authPrincipal.setRoles(roles);
            Set<String> groups = Sets.newHashSet(jwt.getClaims().get("associatedgroup").asArray(String.class));
            authPrincipal.setGroups(groups);
        }catch (JWTVerificationException | JwkException | MalformedURLException verExc) {
            log.warn(verExc.getMessage());
            return null;
        }

        if(!loginName.equals(authPrincipal.getUsername())){
            throw new CfgumAuthenticationException("Given username does not match CFGUM token Username!");
        }
        return authPrincipal;
    }
}
