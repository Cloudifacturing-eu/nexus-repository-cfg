package eu.cloudifacturing.www.security.cfgum.oauth;

import org.sonatype.nexus.security.token.BearerToken;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Named(CfgumToken.NAME)
@Singleton
public final class CfgumToken extends BearerToken {
    public static final String NAME = "CfgumToken";

    public CfgumToken() {super(NAME);}

    @Override
    protected boolean matchesFormat(final List<String> parts) {
        return super.matchesFormat(parts) || !parts.get(1).contains(".");
    }

}
