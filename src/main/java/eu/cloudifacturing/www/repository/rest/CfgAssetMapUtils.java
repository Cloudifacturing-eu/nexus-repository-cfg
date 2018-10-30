package eu.cloudifacturing.www.repository.rest;

import com.google.common.annotations.VisibleForTesting;
import org.sonatype.nexus.repository.rest.SearchUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

@Named
@Singleton
public class CfgAssetMapUtils {

    private static final String EMPTY_PARAM = "";

    private final SearchUtils searchUtils;

    @Inject
    public CfgAssetMapUtils(final SearchUtils searchUtils) {
        this.searchUtils = checkNotNull(searchUtils);
    }

    public static Optional<Object> getValueFromAssetMap(final Map<String, Object> assetMap, final String identifier) {
        if (isNullOrEmpty(identifier) || assetMap.isEmpty()) {
            return Optional.empty();
        }

        List<String> keys = newArrayList(identifier.split("\\."));

        if ("assets".equals(keys.get(0))) {
            keys.remove(0);
        }

        Object value = assetMap;
        for (String key : keys) {
            if (value == null) {
                return Optional.empty();
            }
            value = ((Map<String, Object>) value).get(key);
        }
        return Optional.ofNullable(value);
    }

    public boolean filterAsset(final Map<String, Object> assetMap, final MultivaluedMap<String, String> assetParams) {
        // short circuit if the assetMap contains an assetAttribute found in the list of empty asset params
        if (excludeAsset(assetMap, getEmptyAssetParams(assetParams))) {
            return false;
        }

        // fetch only the set of assetParams that have values
        final Map<String, String> assetParamsWithValues = getNonEmptyAssetParams(assetParams);

        // if no asset parameters were sent, we'll count that as return all assets
        if (assetParamsWithValues.isEmpty()) {
            return true;
        }

        // loop each asset specific http query parameter to filter out assets that do not apply
        return assetParamsWithValues.entrySet().stream()
                .allMatch(entry -> keepAsset(assetMap, entry.getKey(), entry.getValue()));
    }

    private static boolean excludeAsset(final Map<String, Object> assetMap, final List<String> paramFilters) {
        return paramFilters.stream()
                .anyMatch(filter -> getValueFromAssetMap(assetMap, filter).isPresent());
    }

    private static boolean keepAsset(final Map<String, Object> assetMap, final String paramKey, final String paramValue) {
        return getValueFromAssetMap(assetMap, paramKey)
                .map(result -> result.equals(paramValue))
                .orElse(false);
    }

    @VisibleForTesting
    private List<String> getEmptyAssetParams(final MultivaluedMap<String, String> assetParams) {
        return assetParams.entrySet()
                .stream()
                .filter(entry -> EMPTY_PARAM.equals(entry.getValue().get(0)))
                .map(e -> searchUtils.getFullAssetAttributeName(e.getKey()))
                .collect(toList());
    }

    @VisibleForTesting
    private Map<String, String> getNonEmptyAssetParams(final MultivaluedMap<String, String> assetParams) {
        return assetParams.entrySet()
                .stream()
                .filter(entry -> !EMPTY_PARAM.equals(entry.getValue().get(0)))
                .collect(Collectors
                        .toMap(entry -> searchUtils.getFullAssetAttributeName(entry.getKey()), entry -> entry.getValue().get(0)));
    }
}
