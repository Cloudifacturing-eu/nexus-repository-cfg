package eu.cloudifacturing.www.repository.rest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.SecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;
import org.sonatype.nexus.security.authc.apikey.ApiKeyAuthenticationFilter;
import org.sonatype.nexus.siesta.SiestaServlet;

import javax.inject.Named;

/**
 * Set org.sonatype.nexus.siesta.SiestaModule.skip=true in nexus-default.properties to disable Nexus default restful api
 */
@Named
public class RestfulModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(RestfulModule.class);

    private static final String MOUNT_POINT = "/rest";

    public static final String SKIP_MODULE_CONFIGURATION = RestfulModule.class.getName() + ".skip";

    @Override
    protected void configure() {
        if (!Boolean.getBoolean(SKIP_MODULE_CONFIGURATION)) {
            doConfigure();
        }
    }

    private void doConfigure() {
        install(new ServletModule()
        {
            @Override
            protected void configureServlets() {
                log.debug("Mount point: {}", MOUNT_POINT);
                bind(SiestaServlet.class);
                /* Change the default mount point of nexus restful api

                serve(MOUNT_POINT + "/*").with(SiestaServlet.class, ImmutableMap.of(
                        "resteasy.servlet.mapping.prefix", MOUNT_POINT
                ));

                */
                filter(MOUNT_POINT + "/*").through(SecurityFilter.class);
            }
        });

        install(new FilterChainModule()
        {
            @Override
            protected void configure() {
                addFilterChain(MOUNT_POINT + "/**",
                        NexusAuthenticationFilter.NAME,
                        ApiKeyAuthenticationFilter.NAME,
                        AnonymousFilter.NAME,
                        AntiCsrfFilter.NAME);
            }
        });

    }
}
