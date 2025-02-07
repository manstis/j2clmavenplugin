package com.vertispan.j2cl.build;

import java.util.List;

/**
 * Represents a set of sources and the dependencies used to build them. The sourceRoots property
 * can contain directories of sources, or a jar containing sources, but not both - if it doesn't
 * point at a source jar, the contents can be watched for changes.
 */
public class Project implements com.vertispan.j2cl.build.task.Project {
    private final String key;

    private List<? extends com.vertispan.j2cl.build.task.Dependency> dependencies;
    private List<String> sourceRoots;

    public Project(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public List<? extends com.vertispan.j2cl.build.task.Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<? extends com.vertispan.j2cl.build.task.Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getSourceRoots() {
        return sourceRoots;
    }

    public void setSourceRoots(List<String> sourceRoots) {
        this.sourceRoots = sourceRoots;
    }

    @Override
    public boolean hasSourcesMapped() {
        return sourceRoots.stream().noneMatch(root -> root.endsWith(".jar"));
    }

    @Override
    public String toString() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Project project = (Project) o;

        return key.equals(project.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
