package eu.cloudifacturing.www.repository.search;

import com.google.common.collect.ImmutableList;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

/**
 * @since 3.7
 */
@Named("cfg")
@Singleton
public class CfgSearchMappings     extends ComponentSupport
        implements SearchMappings {
    private static final List<SearchMapping> MAPPINGS = ImmutableList.of(
            new SearchMapping("cfg.engineId", "assets.attributes.cfg.engineId", "cfg engineId")
    );

    @Override
    public Iterable<SearchMapping> get() {
        return MAPPINGS;
    }
}
