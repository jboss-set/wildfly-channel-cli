package org.jboss.set.channel.cli.manifestbuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DependencyGroupsMapper {

    private static final YAMLMapper mapper = new YAMLMapper();

    public DependencyGroupsMapper() {
    }

    public static void write(Collection<DependencyGroup> groups, Path output) throws IOException {
        mapper.writeValue(output.toFile(), new ArrayList<>(groups));
    }

    public static String writeAsString(Collection<DependencyGroup> groups) throws IOException {
        return mapper.writeValueAsString(new ArrayList<>(groups));
    }

    public static List<DependencyGroup> read(Path input) throws IOException {
        return mapper.readValue(input.toFile(), new TypeReference<>() {});
    }
}
