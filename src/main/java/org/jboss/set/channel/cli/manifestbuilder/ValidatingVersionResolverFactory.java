package org.jboss.set.channel.cli.manifestbuilder;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.wildfly.channel.Repository;
import org.wildfly.channel.maven.VersionResolverFactory;
import org.wildfly.channel.spi.MavenVersionsResolver;

import java.util.Collection;
import java.util.List;

public class ValidatingVersionResolverFactory implements MavenVersionsResolver.Factory {

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<DependencyGroup> dependencyGroups;

    public ValidatingVersionResolverFactory(RepositorySystem system, RepositorySystemSession session,
                                            List<DependencyGroup> dependencyGroups) {
        this.system = system;
        this.session = session;
        this.dependencyGroups = dependencyGroups;
    }

    @Override
    public MavenVersionsResolver create(Collection<Repository> repositories) {
        try (VersionResolverFactory defaultResolverFactory = new VersionResolverFactory(system, session)) {
            MavenVersionsResolver defaultResolver = defaultResolverFactory.create(repositories);
            return new ValidatingMavenVersionResolver(defaultResolver, dependencyGroups);
        }
    }
}
