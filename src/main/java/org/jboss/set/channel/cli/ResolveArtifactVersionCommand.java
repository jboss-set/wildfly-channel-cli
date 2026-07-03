package org.jboss.set.channel.cli;

import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.set.channel.cli.utils.ConversionUtils;
import org.wildfly.channel.Channel;
import org.wildfly.channel.ChannelSession;
import org.wildfly.channel.MavenArtifact;
import org.wildfly.channel.maven.ChannelCoordinate;
import org.wildfly.channel.maven.VersionResolverFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.List;

@CommandLine.Command(name = "resolve-artifact-version",
        description = "Resolves version of an artifact based on given channels.")
public class ResolveArtifactVersionCommand extends MavenBasedCommand {

    @CommandLine.Parameters(index = "0", description = "Base channel coordinate (URL of GAV).",
            paramLabel = "channelCoordinate")
    private String channelCoordinateString;

    @CommandLine.Parameters(index = "1", description = "Maven artifact G:A.",
            paramLabel = "ga")
    private String ga;

    @CommandLine.Option(names = "--channel-repositories", split = ",",
            description = "Comma separated repositories URLs where the channels should be looked for, if a channel GAV is given.",
            paramLabel = "URL")
    private List<String> channelRepositoriesUrls;

    @CommandLine.Option(names = "--maven-settings",
            description = "Comma separated repositories URLs where the channels should be looked for, if a channel GAV is given.",
            paramLabel = "path")
    private File mavenSettingsPath;

    @Override
    public Integer call() throws Exception {
        final ChannelCoordinate channelCoordinate = ConversionUtils.toChannelCoordinate(channelCoordinateString);
        final List<RemoteRepository> channelRepositories = ConversionUtils.toRepositoryList(channelRepositoriesUrls);
        String[] coords = ga.split(":");

        Settings settings = readMavenSettings(mavenSettingsPath);

        try (VersionResolverFactory resolverFactory = createResolverFactory(system, systemSession, settings)) {
            final List<Channel> channels = resolverFactory.resolveChannels(List.of(channelCoordinate), channelRepositories);
            final ChannelSession channelSession = new ChannelSession(channels, resolverFactory);
            MavenArtifact resolvedArtifact = channelSession.resolveMavenArtifact(coords[0], coords[1], "pom", null, null);
            System.out.println(resolvedArtifact.getVersion());
        }

        return 0;
    }
}
