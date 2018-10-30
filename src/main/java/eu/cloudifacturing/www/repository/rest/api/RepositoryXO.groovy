package eu.cloudifacturing.www.repository.rest.api

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.builder.Builder
import org.sonatype.nexus.repository.Repository

@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode
class RepositoryXO
{
    String name

    String format

    String type

    String url

    static RepositoryXO fromRepository(final Repository repository) {
        return builder()
                .name(repository.getName())
                .format(repository.getFormat().getValue())
                .type(repository.getType().getValue())
                .url(repository.getUrl())
                .build()
    }
}
