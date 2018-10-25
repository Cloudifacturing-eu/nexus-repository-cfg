package eu.cloudifacturing.www.security.cfgum.oauth;

import java.io.Serializable;
import java.util.Set;

public class CfgumPrincipal implements Serializable {
    private String username;
    private char[] oauthToken;
    private Set<String> roles;
    private Set<String> groups;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setOauthToken(char[] oauthToken) {
        this.oauthToken = oauthToken;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public String getUsername() {
        return username;
    }

    public char[] getOauthToken() {
        return oauthToken;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getGroups() { return groups; }

    @Override
    public String toString() {
        return username;
    }
}
