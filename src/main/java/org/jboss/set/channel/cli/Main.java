package org.jboss.set.channel.cli;

import org.jboss.set.channel.cli.manifestbuilder.GenerateDependencyGroupsCommand;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MainCommand());

        commandLine.addSubcommand(new CompareChannelsCommand());
        commandLine.addSubcommand(new CompareManifestsCommand());
        commandLine.addSubcommand(new FindUpgradesCommand());
        commandLine.addSubcommand(new CreateManifestFromRepoCommand());
        commandLine.addSubcommand(new CreateChannelCommand());
        commandLine.addSubcommand(new UpdateChannelCommand());
        commandLine.addSubcommand(new MergeManifestsCommand());
        commandLine.addSubcommand(new ExtractRepositoriesCommand());
        commandLine.addSubcommand(new ExtractManifestUrlCommand());
        commandLine.addSubcommand(new GenerateDependencyGroupsCommand());

//        commandLine.setExecutionExceptionHandler((ex, cmdLine, parseResult) -> CommandLine.ExitCode.SOFTWARE);

        int returnCode = commandLine.execute(args);
        System.exit(returnCode);
    }
}