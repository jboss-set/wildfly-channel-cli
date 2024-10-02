package org.jboss.set.channel.cli.manifestbuilder;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class DependencyGroup {

    private String versionProperty;
    private Set<String> dependencies = new TreeSet<>();

    @SuppressWarnings("unused")
    public DependencyGroup() {
    }

    public DependencyGroup(String versionProperty) {
        this.versionProperty = versionProperty;
    }

    public String getVersionProperty() {
        return versionProperty;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    @SuppressWarnings("unused")
    public void setDependencies(Collection<String> dependencies) {
        this.dependencies = new TreeSet<>(dependencies);
    }

    @SuppressWarnings("unused")
    public void setVersionProperty(String versionProperty) {
        this.versionProperty = versionProperty;
    }
}
