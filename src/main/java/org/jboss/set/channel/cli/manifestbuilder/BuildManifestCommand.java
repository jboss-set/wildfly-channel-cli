package org.jboss.set.channel.cli.manifestbuilder;

import org.jboss.logging.Logger;
import org.jboss.set.channel.cli.MavenBasedCommand;
import org.wildfly.channel.ArtifactCoordinate;
import org.wildfly.channel.Channel;
import org.wildfly.channel.ChannelManifest;
import org.wildfly.channel.ChannelManifestCoordinate;
import org.wildfly.channel.ChannelManifestMapper;
import org.wildfly.channel.ChannelSession;
import org.wildfly.channel.MavenArtifact;
import org.wildfly.channel.Stream;
import org.wildfly.channel.maven.ChannelCoordinate;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "build-manifest",
        description = "Build an up-to-date manifest based on given inputs.")
public class BuildManifestCommand extends MavenBasedCommand {

    private static final Logger logger = Logger.getLogger(BuildManifestCommand.class);

    @CommandLine.Option(names = {"-b", "--base-manifest"}, required = true,
            description = "Path to a manifest containing a list of artifacts that the resulting manifest should contain.")
    private Path baseManifest;

    @CommandLine.Option(names = {"-u", "--upgrade-channel"}, required = true,
            description = "Path to a manifest containing version patterns that should be satisfied when upgrading the base manifest versions.")
    private Path upgradeChannel;

    @CommandLine.Option(names = {"-g", "--dependency-groups"}, required = true,
            description = "Path to a dependency groups file.")
    private Path dependencyGroupFile;

    @CommandLine.Option(names = {"-o", "--output-file"}, defaultValue = "-")
    private Path output;

    private ChannelSession channelSession;

    @Override
    public Integer call() throws Exception {
        initChannelSession();
        final ChannelManifest baseManifest = resolveManifest(new ChannelManifestCoordinate(
                this.baseManifest.toUri().toURL()));

        final List<ArtifactCoordinate> artifactsToResolve = new ArrayList<>();
        for (Stream stream: baseManifest.getStreams()) {
            artifactsToResolve.add(new ArtifactCoordinate(stream.getGroupId(), stream.getArtifactId(), "pom", null,
                    stream.getVersion()));
        }

        final List<MavenArtifact> resolvedArtifacts = channelSession.resolveMavenArtifacts(artifactsToResolve);
        for (int i = 0; i < resolvedArtifacts.size(); i++) {
            ArtifactCoordinate artifactCoordinate = artifactsToResolve.get(i);
            MavenArtifact resolved = resolvedArtifacts.get(i);
            if (!artifactCoordinate.getVersion().equals(resolved.getVersion())) {
                logger.infof("Stream %s:%s:%s upgraded to %s",
                        artifactCoordinate.getGroupId(), artifactCoordinate.getArtifactId(),
                        artifactCoordinate.getVersion(), resolved.getVersion());
            }
        }

        ChannelManifest recordedManifest = channelSession.getRecordedChannel();
        if (output.toString().equals("-")) {
            System.out.println(ChannelManifestMapper.toYaml(recordedManifest));
        } else {
            Files.write(output, ChannelManifestMapper.toYaml(recordedManifest).getBytes());
        }

        return CommandLine.ExitCode.OK;
    }

    private void initChannelSession() throws IOException {
        List<DependencyGroup> dependencyGroups = DependencyGroupsMapper.read(dependencyGroupFile);
        List<Channel> upgradeChannels = resolveChannel(new ChannelCoordinate(upgradeChannel.toUri().toURL()));
        channelSession = new ChannelSession(upgradeChannels, new ValidatingVersionResolverFactory(system, systemSession, dependencyGroups));
    }

}
