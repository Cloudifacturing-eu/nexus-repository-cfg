package eu.cloudifacturing.www.security.cfgum.oauth;

import eu.cloudifacturing.www.security.cfgum.oauth.api.CfgumApiClient;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.authc.NexusApiKeyAuthenticationToken;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.stream.Collectors;

@Singleton
@Named
@Description("CFGUM Token Authentication Realm")
public class CfgumTokenAuthenticationRealm extends AuthorizingRealm {

    private CfgumApiClient cfgumApiClient;
    private static final Logger log= LoggerFactory.getLogger(CfgumTokenAuthenticationRealm.class);
    public static final String NAME = CfgumTokenAuthenticationRealm.class.getName();
    private final String format="CfgumToken";
    @Inject
    public CfgumTokenAuthenticationRealm(CfgumApiClient cfgumApiClient){
        this.cfgumApiClient = cfgumApiClient;
    }

    @Override
    public boolean supports(final AuthenticationToken token) {
        return token instanceof NexusApiKeyAuthenticationToken && format.equals(token.getPrincipal());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void onInit() {
        super.onInit();
        log.info("CFGUM oAuth Realm initialized");
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        CfgumPrincipal user = (CfgumPrincipal) principalCollection.getPrimaryPrincipal();
        log.info("doGetAuthorizationInfo for user {} with roles {} with groups {}", user.getUsername(), user.getRoles().stream().collect(Collectors.joining()),user.getGroups().stream().collect(Collectors.joining()));
        return new SimpleAuthorizationInfo(user.getRoles());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        if (!(token instanceof NexusApiKeyAuthenticationToken)) {
            throw new UnsupportedTokenException(String.format("Token of type %s  is not supported. A %s is required.",
                    token.getClass().getName(), NexusApiKeyAuthenticationToken.class.getName()));
        }
        NexusApiKeyAuthenticationToken t=(NexusApiKeyAuthenticationToken) token;
        log.info("doGetAuthenticationInfo for {}", t.getPrincipal()+":"+t.getCredentials());
        CfgumPrincipal authenticatedPrincipal;

        try {
            log.info("Debug {}");
            authenticatedPrincipal = cfgumApiClient.authz((String)t.getPrincipal(),(char[])(t.getCredentials()));
        } catch (CfgumAuthenticationException e){
            log.warn("Failed authentication");
            log.debug("Failed authentication" + e);
            return null;
        }

        return new SimpleAuthenticationInfo(authenticatedPrincipal,t.getCredentials(),NAME);
    }
}
