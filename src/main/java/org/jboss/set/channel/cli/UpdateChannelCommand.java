package org.jboss.set.channel.cli;

import org.apache.commons.lang3.StringUtils;
import org.jboss.set.channel.cli.utils.ConversionUtils;
import org.jboss.set.channel.cli.utils.IOUtils;
import org.wildfly.channel.BlocklistCoordinate;
import org.wildfly.channel.Channel;
import org.wildfly.channel.ChannelManifestCoordinate;
import org.wildfly.channel.ChannelMapper;
import org.wildfly.channel.Repository;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "update-channel",
        description = "Updates a channel file according to given parameters.")
public class UpdateChannelCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"--output-file", "-o"}, defaultValue = "",
            description = "Channel file to be written. If not set, the channel will be send to stdout.")
    private Path outputFile;

    @CommandLine.Option(names = {"--input-file", "-i"}, required = true,
            description = "Input channel to be modified.")
    private Path inputFile;

    @CommandLine.Option(names = {"--name", "-n"},
            description = "Overrides channel name.")
    private String name;

    @CommandLine.Option(names = {"--description", "-d"},
            description = "Overrides channel description.")
    private String description;

    @CommandLine.Option(names = {"--repositories", "-r"}, split = ",", paramLabel = "ID::URL",
            description = "Overrides channel repositories, provide input in the format `ID1::URL1,ID2::URL2,...`")
    private Set<String> repositoriesStrings;

    @CommandLine.Option(names = {"--add-repositories"}, split = ",", paramLabel = "ID::URL",
            description = "Adds additional channel repositories, provide input in the format `ID1::URL1,ID2::URL2,...`")
    private Set<String> addRepositoriesStrings;

    @CommandLine.Option(names = {"--manifest-coordinate", "-m"},
            description = "Overrides manifest coordinate, GAV or URL.",
            paramLabel = "manifestCoordinate")
    private String manifestCoordinateString;

    @CommandLine.Option(names = {"--blocklist-coordinate", "-b"},
            description = "Overrides blocklist coordinate, GAV or URL.",
            paramLabel = "blocklistCoordinate")
    private String blocklistCoordinateString;

    @CommandLine.Option(names = {"--no-stream-strategy", "-s"},
            description = "Overrides no stream strategy: latest, maven-latest, maven-release or none.",
            paramLabel = "strategy")
    private String noStreamStrategyString;

    @Override
    public Integer call() throws Exception {
        List<Channel> inputChannels = ChannelMapper.fromString(Files.readString(inputFile));
        if (inputChannels.isEmpty()) {
            throw new RuntimeException("The input file doesn't contain any channels.");
        }
        Channel inputChannel = inputChannels.get(0);

        if (StringUtils.isBlank(this.name)) {
            this.name = inputChannel.getName();
        }
        if (StringUtils.isBlank(this.description)) {
            this.description = inputChannel.getDescription();
        }
        if (StringUtils.isBlank(this.manifestCoordinateString)) {
            this.manifestCoordinateString = ConversionUtils.toCoordinateString(inputChannel.getManifestCoordinate());
        }
        if (StringUtils.isBlank(this.blocklistCoordinateString)) {
            this.blocklistCoordinateString = ConversionUtils.toCoordinateString(inputChannel.getBlocklistCoordinate());
        }
        if (StringUtils.isBlank(this.noStreamStrategyString)) {
            this.noStreamStrategyString = ConversionUtils.toNoStreamStrategyString(inputChannel.getNoStreamStrategy());
        }
        if (this.repositoriesStrings == null || this.repositoriesStrings.isEmpty()) {
            this.repositoriesStrings = inputChannel.getRepositories().stream().map(r -> r.getId() + "::" + r.getUrl()).collect(Collectors.toSet());
        }
        if (this.addRepositoriesStrings != null && !this.repositoriesStrings.isEmpty()) {
            this.repositoriesStrings.addAll(addRepositoriesStrings);
        }

        List<Repository> repositories = ConversionUtils.toChannelRepositoryList(new ArrayList<>(repositoriesStrings));
        ChannelManifestCoordinate manifestCoordinate = ConversionUtils.toManifestCoordinate(manifestCoordinateString);
        BlocklistCoordinate blocklistCoordinate = ConversionUtils.toBlocklistCoordinate(blocklistCoordinateString);
        Channel.NoStreamStrategy noStreamStrategy = ConversionUtils.toNoStreamStrategy(noStreamStrategyString);

        Channel channel = new Channel(name, description, null, repositories, manifestCoordinate, blocklistCoordinate, noStreamStrategy);
        if (outputFile.equals(Path.of(""))) {
            System.out.println(ChannelMapper.toYaml(channel));
        } else {
            IOUtils.writeChannelFile(outputFile, channel);
        }

        return CommandLine.ExitCode.OK;
    }
}
