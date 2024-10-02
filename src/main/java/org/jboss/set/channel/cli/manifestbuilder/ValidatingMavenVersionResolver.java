package org.jboss.set.channel.cli.manifestbuilder;

import org.jboss.logging.Logger;
import org.wildfly.channel.ArtifactCoordinate;
import org.wildfly.channel.ArtifactTransferException;
import org.wildfly.channel.ChannelMetadataCoordinate;
import org.wildfly.channel.spi.MavenVersionsResolver;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ValidatingMavenVersionResolver implements MavenVersionsResolver {

    private static final Logger logger = Logger.getLogger(ValidatingMavenVersionResolver.class);

    private final MavenVersionsResolver defaultResolver;
    private final List<DependencyGroup> dependencyGroups;
    private final Map<String, Set<String>> defaultResolverCache = new ConcurrentHashMap<>(); // "G:A" => Set of available versions
    private final Map<String, Set<String>> resultCache = new ConcurrentHashMap<>(); // "G:A" => Set of available versions

    public ValidatingMavenVersionResolver(MavenVersionsResolver defaultResolver, List<DependencyGroup> dependencyGroups) {
        this.defaultResolver = defaultResolver;
        this.dependencyGroups = dependencyGroups;
    }

    @Override
    public Set<String> getAllVersions(String groupId, String artifactId, String extension, String classifier) {
        String ga = groupId + ":" + artifactId;
        Optional<DependencyGroup> groupOptional = dependencyGroups.stream()
                .filter(g -> g.getDependencies().contains(ga))
                .findFirst();

        if (groupOptional.isPresent()) {
            // Will contain discovered valid versions for given GA
            Set<String> result = resultCache.get(ga);
            if (result != null) {
                return result;
            }

            final DependencyGroup group = groupOptional.get();
            for (String otherGA: group.getDependencies()) {
                final String[] split = otherGA.split(":");
                assert split.length == 2;
                final String g = split[0];
                final String a = split[1];

                final Set<String> versions = defaultResolverCache.computeIfAbsent(otherGA, key -> defaultResolver.getAllVersions(g, a, "pom", null));
                if (result == null) {
                    // Initialize the result set with the versions of the first item in the list.
                    result = new HashSet<>(versions);
                } else {
                    Set<String> removedVersions = new HashSet<>(result);

                    // If already initialized, start eliminating the version list to versions available for all the
                    // artifacts in the group.
                    result.retainAll(versions);

                    // Obtain a list of versions removed in this round and report
                    removedVersions.removeAll(result);
                    onVersionsRefused(removedVersions, group.getVersionProperty(), g, a);
                }
            }

            if (result != null) {
                for (String otherGA: group.getDependencies()) {
                    resultCache.put(otherGA, result);
                }
                return result;
            }
        }
        // If there was no dependency group for given artifact, delegate to the original impl.
        return defaultResolver.getAllVersions(groupId, artifactId, extension, classifier);
    }

    @Override
    public File resolveArtifact(String groupId, String artifactId, String extension, String classifier, String version) throws ArtifactTransferException {
        return defaultResolver.resolveArtifact(groupId, artifactId, extension, classifier, version);
    }

    @Override
    public List<File> resolveArtifacts(List<ArtifactCoordinate> coordinates) throws ArtifactTransferException {
        return defaultResolver.resolveArtifacts(coordinates);
    }

    @Override
    public List<URL> resolveChannelMetadata(List<? extends ChannelMetadataCoordinate> manifestCoords) throws ArtifactTransferException {
        return defaultResolver.resolveChannelMetadata(manifestCoords);
    }

    @Override
    public String getMetadataReleaseVersion(String groupId, String artifactId) {
        return defaultResolver.getMetadataReleaseVersion(groupId, artifactId);
    }

    @Override
    public String getMetadataLatestVersion(String groupId, String artifactId) {
        return defaultResolver.getMetadataLatestVersion(groupId, artifactId);
    }

    private void onVersionsRefused(Collection<String> versions, String versionProperty, String groupId, String artifactId) {
        if (!versions.isEmpty()) {
            logger.warnf("Versions %s not applicable for group %s, because artifact %s:%s is not available.",
                    String.join(", ", versions), versionProperty, groupId, artifactId);
        }
    }

}
