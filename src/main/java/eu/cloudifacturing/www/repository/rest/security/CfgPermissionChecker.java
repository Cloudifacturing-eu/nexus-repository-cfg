package eu.cloudifacturing.www.repository.rest.security;

import org.sonatype.nexus.repository.storage.Asset;

public interface CfgPermissionChecker {
    void ensurePermitted(String path);
}
