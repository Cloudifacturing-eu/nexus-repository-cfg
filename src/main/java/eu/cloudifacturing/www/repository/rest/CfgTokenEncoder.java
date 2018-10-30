package eu.cloudifacturing.www.repository.rest;

import org.elasticsearch.index.query.QueryBuilder;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.io.Hex;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_ACCEPTABLE;

@Singleton
@Named
public class CfgTokenEncoder extends ComponentSupport {
    public int decode(@Nullable final String continuationToken, final QueryBuilder query) {
        if (continuationToken == null) {
            return 0;
        }
        else {
            String decoded = new String(Hex.decode(continuationToken), UTF_8);
            String[] decodedParts = decoded.split(":");
            if (decodedParts.length != 2) {
                throw new WebApplicationException(format("Unable to parse token %s", continuationToken), NOT_ACCEPTABLE);
            }
            if (!decodedParts[1].equals(getHashCode(query))) {
                throw new WebApplicationException(
                        format("Continuation token %s does not match this query", continuationToken), NOT_ACCEPTABLE);
            }
            return parseInt(decodedParts[0]);
        }
    }

    public String encode(final int lastFrom, final int pageSize, final QueryBuilder query) {
        int index = lastFrom + pageSize;
        return Hex.encode(format("%s:%s", Integer.toString(index), getHashCode(query)).getBytes(UTF_8));
    }

    private String getHashCode(final QueryBuilder query) {
        return MD5.function().hashString(query.toString(), UTF_8).toString();
    }
}
