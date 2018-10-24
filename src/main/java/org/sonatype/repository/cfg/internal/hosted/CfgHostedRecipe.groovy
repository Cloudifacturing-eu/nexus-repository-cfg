package org.sonatype.repository.cfg.internal.hosted;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.http.HttpHandlers;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.SingleAssetComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.*;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.SuffixMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher
import org.sonatype.repository.cfg.internal.CfgFormat
import org.sonatype.repository.cfg.internal.security.CfgSecurityFacet;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import static org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and;

@Named(CfgHostedRecipe.NAME)
@Singleton
class CfgHostedRecipe extends RecipeSupport {
    public static final String NAME = 'cfg-hosted'

    @Inject
    Provider<CfgSecurityFacet> securityFacet

    @Inject
    Provider<ConfigurableViewFacet> viewFacet

    @Inject
    Provider<CfgHostedFacetImpl> cfgHostedFacet

    @Inject
    Provider<StorageFacet> storageFacet

    @Inject
    Provider<AttributesFacet> attributesFacet

    @Inject
    Provider<SingleAssetComponentMaintenance> componentMaintenance

    @Inject
    Provider<SearchFacet> searchFacet

    @Inject
    ExceptionHandler exceptionHandler

    @Inject
    TimingHandler timingHandler

    @Inject
    IndexHtmlForwardHandler indexHtmlForwardHandler

    @Inject
    SecurityHandler securityHandler

    @Inject
    PartialFetchHandler partialFetchHandler

    @Inject
    UnitOfWorkHandler unitOfWorkHandler

    @Inject
    HostedHandlers hostedHandlers

    @Inject
    ConditionalRequestHandler conditionalRequestHandler

    @Inject
    ContentHeadersHandler contentHeadersHandler

    @Inject
    HandlerContributor handlerContributor

    @Inject
    CfgHostedRecipe(@Named(HostedType.NAME) final Type type,
                    @Named(CfgFormat.NAME) final Format format)
    {
        super(type, format)
    }

    @Override
    void apply(@Nonnull final Repository repository) throws Exception {
        repository.attach(securityFacet.get())
        repository.attach(configure(viewFacet.get()))
        repository.attach(cfgHostedFacet.get())
        repository.attach(storageFacet.get())
        repository.attach(attributesFacet.get())
        repository.attach(componentMaintenance.get())
        repository.attach(searchFacet.get())
    }

    /**
     * Configure {@link ViewFacet}.
     */
    private ViewFacet configure(final ConfigurableViewFacet facet) {
        Router.Builder builder = new Router.Builder()

        // handle GET / forwards to /index.html
        builder.route(new Route.Builder()
                .matcher(and(new ActionMatcher(HttpMethods.GET), new SuffixMatcher('/')))
                .handler(timingHandler)
                .handler(indexHtmlForwardHandler)
                .create()
        )

        builder.route(new Route.Builder()
                .matcher(new TokenMatcher('/{name:.+}'))
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(exceptionHandler)
                .handler(handlerContributor)
                .handler(conditionalRequestHandler)
                .handler(partialFetchHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(hostedHandlers)
                .create())

        builder.defaultHandlers(HttpHandlers.badRequest())

        facet.configure(builder.create())

        return facet
    }
}
