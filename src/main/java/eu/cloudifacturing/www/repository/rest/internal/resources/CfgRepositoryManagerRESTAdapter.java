package eu.cloudifacturing.www.repository.rest.internal.resources;

import org.sonatype.nexus.repository.Repository;

import java.util.List;

public interface CfgRepositoryManagerRESTAdapter {
    Repository getRepository(String repositoryId);

    /**
     * Retrieve all repositories that the user access to.
     */
    List<Repository> getRepositories();
}
