package eu.cloudifacturing.www.repository.rest.api

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.builder.Builder
import org.sonatype.nexus.repository.Repository
import eu.cloudifacturing.www.repository.rest.api.RepositoryItemIDXO
import org.sonatype.nexus.repository.storage.Asset

import static org.sonatype.nexus.common.entity.EntityHelper.id
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.ID
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME
import static org.sonatype.nexus.repository.storage.Asset.CHECKSUM
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES

@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(includes = ['id'])
class ArtefactXO {
    String downloadUrl

    String path

    String id

    String repository

    String format

    Map checksum

    Map metadata

    Map cfg

    static ArtefactXO fromAsset(final Asset asset, final Repository repository) {
        String internalId = id(asset).getValue()

        Map checksum = asset.attributes().child(CHECKSUM).backing()

        return builder()
                .path(asset.name())
                .downloadUrl(repository.url + '/' + asset.name())
                .id(new RepositoryItemIDXO(repository.name, internalId).value)
                .repository(repository.name)
                .checksum(checksum)
                .format(repository.format.value)
                .cfg(asset.attributes().child("cfg").backing())
                .metadata(asset.attributes().child("metadata").backing())
                .build()
    }

    static ArtefactXO fromElasticSearchMap(final Map map, final Repository repository) {
        String internalId = (String) map.get(ID)

        Map checksum = (Map) map.get(P_ATTRIBUTES, [:])[CHECKSUM]

        return builder()
                .path((String) map.get(NAME))
                .downloadUrl(repository.url + '/' + (String) map.get(NAME))
                .id(new RepositoryItemIDXO(repository.name, internalId).value)
                .repository(repository.name)
                .checksum(checksum)
                .format(repository.format.value)
                .cfg((Map) map.get("attributes",[:])["cfg"])
                .metadata((Map) map.get("attributes",[:])["metadata"])
                .build()
    }
}
