package org.deer.dependency.graph;

import lombok.Getter;
import org.apache.maven.artifact.Artifact;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ResolveContext {

    private final List<Artifact> failedArtifacts = new ArrayList<>();
    private final List<Artifact> processedArtifacts = new ArrayList<>();

    public void addFailedArtifact(Artifact artifact) {
        failedArtifacts.add(artifact);
    }

    public void addProcessedArtifact(Artifact artifact) {
        processedArtifacts.add(artifact);
    }

    public boolean isNotFailedArtifact(Artifact artifact) {
        return !failedArtifacts.contains(artifact);
    }

    public boolean isProcessedArtifact(Artifact artifact) {
        return processedArtifacts.stream().anyMatch(processed -> processed.compareTo(artifact) == 0);
    }

}
