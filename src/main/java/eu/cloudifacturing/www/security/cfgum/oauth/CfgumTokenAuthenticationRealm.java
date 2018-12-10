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

    @Inject
    public CfgumTokenAuthenticationRealm(CfgumApiClient cfgumApiClient){
        this.cfgumApiClient = cfgumApiClient;
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
        log.info(token.getClass().getName());
        if (!(token instanceof UsernamePasswordToken)) {
            throw new UnsupportedTokenException(String.format("Token of type %s  is not supported. A %s is required.",
            token.getClass().getName(), UsernamePasswordToken.class.getName()));
        }
        UsernamePasswordToken t = (UsernamePasswordToken) token;
        log.info("doGetAuthenticationInfo for {}", ((UsernamePasswordToken) token).getUsername());
        CfgumPrincipal authenticatedPrincipal;
        try {
            authenticatedPrincipal = cfgumApiClient.authz(t.getUsername(),t.getPassword());
            if(authenticatedPrincipal==null){
                return null;
            } else {
                log.info("Successfully authenticated {}", t.getUsername());
            }
        } catch (CfgumAuthenticationException e){
            log.warn("Failed authentication");
            log.debug("Failed authentication" + e);
            return null;
        }

        return new SimpleAuthenticationInfo(authenticatedPrincipal,t.getCredentials(),NAME);
    }
}
