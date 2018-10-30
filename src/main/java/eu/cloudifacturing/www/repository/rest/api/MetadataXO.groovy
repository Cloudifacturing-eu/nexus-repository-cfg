package eu.cloudifacturing.www.repository.rest.api

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.builder.Builder
import org.sonatype.nexus.common.entity.EntityHelper
import org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer
import org.sonatype.nexus.repository.storage.Asset

@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(includes = ['id'])
class MetadataXO {
    Map metadata

    static MetadataXO fromAssetMetadata(final Asset asset) {
        String internalId = EntityHelper.id(asset).getValue()

        return builder()
                .metadata(ImmutableMap.<String, Object>builder().putAll(asset.attributes().child("metadata").backing()).putAll(asset.attributes().child("cfg").backing()).build())
                .build()
    }

    static MetadataXO fromElasticSearchMap(final Map map) {
        String internalId = (String) map.get(DefaultComponentMetadataProducer.ID)

        return builder()
                .metadata(ImmutableMap.<String, Object>builder().putAll((Map) map.get("attributes",[:])["metadata"]).putAll((Map) map.get("attributes",[:])["cfg"]).build())
                .build()
    }
}
