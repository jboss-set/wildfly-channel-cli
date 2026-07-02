package org.jboss.set.channel.cli;

import org.wildfly.channel.version.VersionMatcher;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sort-versions",
        description = "Reads version strings from stdin (whitespace-delimited) and outputs them sorted from lowest to highest.")
public class SortVersionsCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        List<String> versions = new ArrayList<>();
        
        // Read from stdin
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Split by whitespace and add non-empty strings
                String[] parts = line.trim().split("\\s+");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        versions.add(part);
                    }
                }
            }
        }
        
        // Sort using VersionMatcher.COMPARATOR
        versions.sort(VersionMatcher.COMPARATOR);
        
        // Output sorted versions
        for (String version : versions) {
            System.out.println(version);
        }
        
        return CommandLine.ExitCode.OK;
    }
}
