package org.deer.dependency.graph;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.*;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactResolver;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

@Slf4j
@Mojo(name = "resolve-graph", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ResolveGraphMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    public MavenSession session;

    @Component
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ProjectBuilder projectBuilder;

    @SneakyThrows
    public void execute() {
        getLog().info("Starting to resolve dependency graph");

        final Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "maven-dependencies"));

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(project);
        buildingRequest.setResolveDependencies(true);
        buildingRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        final ResolveContext resolveContext = new ResolveContext();

        try (Session neoSession = driver.session()) {
            final DependencyNode dependencyNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, artifact -> true);
            dependencyNode.accept(new NodeAndRelationshipWritingNodeVisitor(neoSession, project.getDependencies(), false, false));

            //exclude root artifact from duplicate processing
            project.getArtifacts()
                    .stream()
                    .filter(artifact -> new EqualsBuilder()
                            .append(artifact.getGroupId(), project.getGroupId())
                            .append(artifact.getArtifactId(), project.getArtifactId())
                            .append(artifact.getVersion(), project.getVersion())
                            .isEquals())
                    .findFirst()
                    .ifPresent(resolveContext::addProcessedArtifact);

            project.getArtifacts().forEach(artifact -> projectBuildForArtifact(artifact, neoSession, resolveContext));
        }

        driver.close();
    }


    private void projectBuildForArtifact(Artifact artifact, Session neoSession, ResolveContext resolveContext) {
        if (resolveContext.isProcessedArtifact(artifact)) {
            return;
        }
        resolveContext.addProcessedArtifact(artifact);

        ProjectBuildingRequest depBuildRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        depBuildRequest.setProject(null);
        depBuildRequest.setResolveDependencies(true);
        depBuildRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        MavenProject mavenProject;
        try {
            mavenProject = projectBuilder.build(artifact, depBuildRequest).getProject();

            depBuildRequest.setProject(mavenProject);

            log.info("Processing child project {}:{}:{}", mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion());

            final DependencyNode dependencyNode = dependencyGraphBuilder.buildDependencyGraph(depBuildRequest, art -> true);
            dependencyNode.accept(new NodeAndRelationshipWritingNodeVisitor(neoSession, mavenProject.getDependencies(), true, false));
            mavenProject.getArtifacts()
                    .stream()
                    .filter(resolveContext::isNotFailedArtifact)
                    .forEach(art -> projectBuildForArtifact(art, neoSession, resolveContext));
        } catch (ProjectBuildingException e) {
            resolveContext.addFailedArtifact(artifact);
            getLog().warn("Error resolving project for artifact " + artifact);
        } catch (DependencyGraphBuilderException e) {
            resolveContext.addFailedArtifact(artifact);
            getLog().warn("Error resolving dependency tree for artifact " + artifact);
        }
    }
}
