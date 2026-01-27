package org.jboss.set.channel.cli;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.wildfly.channel.ChannelManifest;
import org.wildfly.channel.ChannelManifestMapper;
import org.wildfly.channel.Stream;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "create-manifest-from-repo",
        description = "Scans a local maven repository and creates a manifest file representing the GAVs existing in the repository.")
public class CreateManifestFromRepoCommand implements Callable<Integer> {

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    @CommandLine.Parameters(index = "0",
            description = "Local Maven repository path to generate the manifest from.",
            paramLabel = "path")
    private Path repositoryPath;

    @CommandLine.Option(names = {"--output-file", "-o"}, defaultValue = "manifest.yaml",
            description = "Manifest file to be written.")
    private Path outputFile;

    @Override
    public Integer call() throws Exception {
        ArrayList<Stream> streams = new ArrayList<>();

        try (java.util.stream.Stream<Path> stream = Files.walk(repositoryPath)) {
            List<Path> metadataFiles = stream.filter(p -> MAVEN_METADATA_XML.equals(p.getFileName().toString()))
                    .toList();

            for (Path metadataPath : metadataFiles) {
                try (InputStream is = new FileInputStream(metadataPath.toFile())) {
                    MetadataXpp3Reader reader = new MetadataXpp3Reader();
                    Metadata metadata = reader.read(is);
                    if (metadata.getVersion() != null) {
                        // Skip metadata files that list specific artifact files, we are just interested in versions.
                        continue;
                    }
                    if (metadata.getVersioning() != null) {
                        String version = metadata.getVersioning().getVersions().getLast();
                        Path artifactDir = metadataPath.getParent().resolve(version);
                        Set<String> extensions = listExtensions(artifactDir);
                        if (extensions.contains("jar") || extensions.contains("zip")) {
                            streams.add(new Stream(metadata.getGroupId(), metadata.getArtifactId(), version));
                        }
                    }
                }
            }
        }

        ChannelManifest manifest = new ChannelManifest("generated manifest", null, null, streams);
        String yaml = ChannelManifestMapper.toYaml(manifest);
        Files.writeString(outputFile, yaml, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        return CommandLine.ExitCode.OK;
    }

    /**
     * Lists extensions of files present in the directory.
     */
    private Set<String> listExtensions(Path dir) throws IOException {
        HashSet<String> extensions = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            stream.iterator().forEachRemaining(file -> {
                if (!Files.isDirectory(file)) {
                    String filename = file.getFileName().toString();
                    int dotIndex = filename.lastIndexOf(".");
                    if (dotIndex > 0) {
                        extensions.add(filename.substring(dotIndex + 1));
                    }
                }
            });
        }
        return extensions;
    }
}
