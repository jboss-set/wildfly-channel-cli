package org.jboss.set.channel.cli;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jboss.logging.Logger;
import org.wildfly.channel.Channel;
import org.wildfly.channel.ChannelManifest;
import org.wildfly.channel.ChannelManifestCoordinate;
import org.wildfly.channel.ChannelManifestMapper;
import org.wildfly.channel.ChannelMapper;
import org.wildfly.channel.Repository;
import org.wildfly.channel.Stream;
import org.wildfly.channel.maven.ChannelCoordinate;
import org.wildfly.channel.maven.VersionResolverFactory;
import org.wildfly.channel.spi.MavenVersionsResolver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.wildfly.channel.maven.VersionResolverFactory.DEFAULT_REPOSITORY_POLICY;

abstract class MavenBasedCommand implements Callable<Integer> {

    protected static final Logger logger = Logger.getLogger(MavenBasedCommand.class);


    protected final RepositorySystem system;
    protected final DefaultRepositorySystemSession systemSession;

    public MavenBasedCommand() {
        try {
            system = newRepositorySystem();
            systemSession = newRepositorySystemSession(system);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize command", e);
        }
    }

    protected static Set<Stream> resolveStreams(List<Channel> channels, VersionResolverFactory resolverFactory) {
        List<ChannelManifestCoordinate> manifestCoordinates = channels.stream()
                .map(Channel::getManifestCoordinate).toList();
        List<Repository> repositories = channels.stream()
                .flatMap(channel -> channel.getRepositories().stream()).toList();

        return resolveStreams(manifestCoordinates, repositories, resolverFactory);
    }

    protected static Set<Stream> resolveStreams(List<ChannelManifestCoordinate> manifestCoordinates, List<Repository> repositories, VersionResolverFactory resolverFactory) {
        Channel.Builder builder = new Channel.Builder();
        for (Repository repository : repositories) {
            builder.addRepository(repository.getId(), repository.getUrl());
        }
        try (MavenVersionsResolver resolver = resolverFactory.create(builder.build())) {
            List<URL> resolvedBaseManifests = resolver.resolveChannelMetadata(manifestCoordinates);
            List<ChannelManifest> baseManifests = resolvedBaseManifests.stream().map(ChannelManifestMapper::from).toList();
            return baseManifests.stream().flatMap(manifest -> manifest.getStreams().stream()).collect(Collectors.toSet());
        }
    }

    protected ChannelManifest resolveManifest(ChannelManifestCoordinate coordinate, List<Repository> repositories) {
        return ChannelManifestMapper.from(resolveManifestUrl(coordinate, repositories));
    }

    protected URL resolveManifestUrl(ChannelManifestCoordinate coordinate, List<Repository> repositories) {
        try (MavenVersionsResolver resolver = createResolver(system, systemSession, repositories)) {
            List<URL> urls = resolver.resolveChannelMetadata(List.of(coordinate));
            return urls.get(0);
        }
    }

    protected Channel resolveChannel(ChannelCoordinate coordinate, List<Repository> repositories) {
        try (MavenVersionsResolver resolver = createResolver(system, systemSession, repositories)) {
            List<URL> urls = resolver.resolveChannelMetadata(List.of(coordinate));
            return ChannelMapper.from(urls.get(0));
        }
    }

    private static MavenVersionsResolver createResolver(RepositorySystem system, RepositorySystemSession systemSession,
                                                        List<Repository> repositories) {
        try (VersionResolverFactory resolverFactory = new VersionResolverFactory(system, systemSession)) {
            Channel.Builder builder = new Channel.Builder();
            for (Repository repository : repositories) {
                builder.addRepository(repository.getId(), repository.getUrl());
            }
            return resolverFactory.create(builder.build());
        }
    }

    protected static VersionResolverFactory createResolverFactory(RepositorySystem system, RepositorySystemSession session, Settings settings) {

        Function<Repository, RemoteRepository> repositoryFactory = new Function<Repository, RemoteRepository>() {
            @Override
            public RemoteRepository apply(Repository repository) {
                RemoteRepository.Builder builder =
                        new RemoteRepository.Builder(repository.getId(), "default", repository.getUrl())
                                .setPolicy(DEFAULT_REPOSITORY_POLICY);

                if (settings != null) {
                    Server server = settings.getServer(repository.getId());
                    if (server != null && (server.getUsername() != null || server.getPassword() != null)) {
                        builder.setAuthentication(new AuthenticationBuilder()
                                        .addUsername(server.getUsername())
                                        .addPassword(server.getPassword())
                                        .build())
                                .build();
                    }
                }
                return builder.build();
            }
        };

        return new VersionResolverFactory(system, session, null, repositoryFactory);
    }

    @SuppressWarnings("deprecation")
    protected static RepositorySystem newRepositorySystem() {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                logger.error("A Maven service creation failed", exception);
            }
        });
        return locator.getService(RepositorySystem.class);
    }

    protected static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) throws IOException {
        // TODO: Create empty temporary dir to act as local maven repo - we don't want any artifacts loaded from the
        //  local repo, because the channel would filter those out, leaving us with no artifact available.
        //  There is a PR open that could resolve this issue: https://github.com/wildfly-extras/wildfly-channel/pull/218
        //  If that's merged we could try to use standard local maven repo here.
        FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(
                PosixFilePermissions.fromString("rwx------"));
        Path localRepoPath = Files.createTempDirectory("local-maven-cache-", attrs);

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepoPath.toFile());
//        LocalRepository localRepo = new LocalRepository(LOCAL_MAVEN_REPO.toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        // TODO: For some reason I get metadata checksum failures from MRRC, even though when I check them manually the
        //  checksum seems to be correct.
        session.setChecksumPolicy("warn");

        return session;
    }

    protected static Settings readMavenSettings(File settingsPath) throws IOException {
        DefaultSettingsReader settingsReader = new DefaultSettingsReader();
        return settingsReader.read(settingsPath, null);
    }

}
