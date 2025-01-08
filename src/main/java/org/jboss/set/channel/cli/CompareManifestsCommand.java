package org.jboss.set.channel.cli;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.set.channel.cli.utils.ConversionUtils;
import org.wildfly.channel.ChannelManifestCoordinate;
import org.wildfly.channel.Repository;
import org.wildfly.channel.Stream;
import org.wildfly.channel.maven.VersionResolverFactory;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@CommandLine.Command(name = "compare-manifests",
        description = "Generates report that identifies intersecting streams of two given manifests, and highlights " +
                "streams where their versions differ.")
public class CompareManifestsCommand extends MavenBasedCommand {

    @CommandLine.Parameters(index = "0", description = "Base manifest coordinate (URL of GAV)",
            paramLabel = "manifestCoordinate")
    private String baseManifestCoordinate;

    @CommandLine.Parameters(index = "1", description = "Comparison manifest coordinate (URL or GAV)",
            paramLabel = "manifestCoordinate")
    private String targetManifestCoordinate;

    @CommandLine.Option(names = "--repositories",
            description = "Comma separated repositories URLs where the manifests should be looked for",
            split = ",",
            paramLabel = "URL")
    private List<String> channelRepositoriesUrls;

    @Override
    public Integer call() throws Exception {
        final ChannelManifestCoordinate baseCoordinate = ConversionUtils.toManifestCoordinate(baseManifestCoordinate);
        final ChannelManifestCoordinate targetCoordinate = ConversionUtils.toManifestCoordinate(targetManifestCoordinate);
        final List<RemoteRepository> channelRepositories = ConversionUtils.toRepositoryList(channelRepositoriesUrls);

        try (VersionResolverFactory resolverFactory = new VersionResolverFactory(system, systemSession)) {
            List<Repository> repositories = ConversionUtils.toChannelRepositories(channelRepositories);
            Set<Stream> baseStreams = resolveStreams(Collections.singletonList(baseCoordinate), repositories, resolverFactory);
            Set<Stream> targetStreams = resolveStreams(Collections.singletonList(targetCoordinate), repositories, resolverFactory);

            ArrayList<Triple<String, String, String>> differentItems = new ArrayList<>();
            ArrayList<Pair<String, String>> missingItems = new ArrayList<>();
            ArrayList<Pair<String, String>> addedItems = new ArrayList<>();
            for (Stream stream1 : baseStreams) {
                Optional<Stream> stream2Opt = targetStreams.stream().filter(s -> s.getGroupId().equals(stream1.getGroupId())
                        && s.getArtifactId().equals(stream1.getArtifactId())).findFirst();
                if (stream2Opt.isEmpty()) {
                    missingItems.add(Pair.of(stream1.getGroupId() + ":" + stream1.getArtifactId(), stream1.getVersion()));
                    continue;
                }

                Stream stream2 = stream2Opt.get();
                boolean differ = stream1.getVersion() != null && !stream1.getVersion().equals(stream2.getVersion());
                if (differ) {
                    differentItems.add(Triple.of(stream1.getGroupId() + ":" + stream1.getArtifactId(), stream1.getVersion(), stream2.getVersion()));
                }
            }

            for (Stream stream2: targetStreams) {
                boolean match = baseStreams.stream().anyMatch(s -> s.getGroupId().equals(stream2.getGroupId())
                        && s.getArtifactId().equals(stream2.getArtifactId()));
                if (!match) {
                    addedItems.add(Pair.of(stream2.getGroupId() + ":" + stream2.getArtifactId(), stream2.getVersion()));
                }
            }

            if (!missingItems.isEmpty()) {
                System.out.println("Missing streams:");
                int gaLength = missingItems.stream().map(i -> i.getLeft().length()).max(Comparator.naturalOrder()).orElse(30);
                for (Pair<String, String> i: missingItems) {
                    System.out.print(StringUtils.rightPad(i.getLeft(), gaLength + 2));
                    System.out.println(i.getRight());
                }
            }

            if (!differentItems.isEmpty()) {
                System.out.println("Versions differ:");
                int gaLength = differentItems.stream().map(i -> i.getLeft().length()).max(Comparator.naturalOrder()).orElse(30);
                int vLength = differentItems.stream().map(i -> i.getMiddle().length()).max(Comparator.naturalOrder()).orElse(10);
                for (Triple<String, String, String> i: differentItems) {
                    System.out.print(StringUtils.rightPad(i.getLeft(), gaLength + 2));
                    System.out.print(StringUtils.rightPad(i.getMiddle(), vLength + 2));
                    System.out.println(i.getRight());
                }
            }

            if (!addedItems.isEmpty()) {
                System.out.println("Added streams:");
                int gaLength = addedItems.stream().map(i -> i.getLeft().length()).max(Comparator.naturalOrder()).orElse(30);
                for (Pair<String, String> i: addedItems) {
                    System.out.print(StringUtils.rightPad(i.getLeft(), gaLength + 2));
                    System.out.println(i.getRight());
                }
            }

            return CommandLine.ExitCode.OK;
        }
    }
}
