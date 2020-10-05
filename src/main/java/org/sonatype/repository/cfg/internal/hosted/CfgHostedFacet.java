package org.sonatype.repository.cfg.internal.hosted;

import com.fasterxml.jackson.core.JsonParseException;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import javax.annotation.Nullable;
import java.io.IOException;

@Exposed
public interface CfgHostedFacet extends Facet {
    @Nullable
    Content get(String path) throws IOException;

    Content put(String path, AttributesMap attributesMap, Payload content) throws IOException;

    boolean delete(String path) throws IOException;

    void setCacheInfo(String path, Content content, CacheInfo cacheInfo) throws IOException;

    Asset getOrCreateAsset(Repository repository, String componentName, String componentGroup, String assetName);
}
