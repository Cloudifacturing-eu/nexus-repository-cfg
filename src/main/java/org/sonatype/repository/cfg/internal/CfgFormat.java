package org.sonatype.repository.cfg.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;

/**
 * Cfg repository format.
 */
@Named(CfgFormat.NAME)
@Singleton
public class CfgFormat extends Format{
    public static final String NAME = "cfg";
    public CfgFormat() {
        super(NAME);
    }
}
