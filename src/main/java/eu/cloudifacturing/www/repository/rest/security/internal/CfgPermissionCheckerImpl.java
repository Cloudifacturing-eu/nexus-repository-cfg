package eu.cloudifacturing.www.repository.rest.security.internal;

import eu.cloudifacturing.www.repository.rest.security.CfgPermissionChecker;
import eu.cloudifacturing.www.security.cfgum.oauth.CfgumPrincipal;
import org.sonatype.nexus.security.SecurityHelper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Set;

@Named
@Singleton
public class CfgPermissionCheckerImpl implements CfgPermissionChecker {
    private final SecurityHelper securityHelper;

    @Inject
    public CfgPermissionCheckerImpl(final SecurityHelper securityHelper){
        this.securityHelper = securityHelper;
    }

    @Override
    public void ensurePermitted(String path) {
        if(securityHelper.subject().getPrincipal() instanceof CfgumPrincipal){
            boolean permitted =false;
            Set<String> group = ((CfgumPrincipal)securityHelper.subject().getPrincipal()).getGroups();
            for(String g:group){
                if(g.substring(1).equals(path)){
                    permitted=true;
                }
            }
            if(!permitted){
                throw new WebApplicationException("Error: You do not have permission to moderate in path: "+path,Response.Status.FORBIDDEN);
            } else {
                System.out.println(path + " permitted:" + permitted);
            }
        }
    }
}
