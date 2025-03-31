package org.jboss.set.channel.cli.manifestbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DependencyGroup {

    private final String component;
    private final List<String> aliases = new ArrayList<>();
    private final Set<String> dependencies = new TreeSet<>();

    public DependencyGroup(String component) {
        this.component = component;
    }

    public String getComponent() {
        return component;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public List<String> getAliases() {
        return aliases;
    }
}
