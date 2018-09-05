/**
 * (C) Copyright IBM Corporation 2014, 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.maven.plugins;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.pluginsupport.MojoSupport;
import org.codehaus.mojo.pluginsupport.ant.AntHelper;
import org.codehaus.mojo.pluginsupport.util.ArtifactItem;

/**
 * Liberty Abstract Mojo Support
 * 
 */
public abstract class AbstractLibertySupport extends MojoSupport {
    /**
     * Maven Project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project = null;
    
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    protected ArtifactRepository artifactRepository = null;
    
    /**
     * The build settings.
     */
    @Parameter(defaultValue = "${settings}", required = true, readonly = true)
    protected Settings settings;
    
    @Component(role = AntHelper.class)
    protected AntHelper ant;
    
    @Component
    protected RepositorySystem repositorySystem;
    
    @Component
    protected ProjectBuilder mavenProjectBuilder;
    
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;
    
    @Parameter(property = "reactorProjects", required = true, readonly = true)
    protected List<MavenProject> reactorProjects;
    
    protected MavenProject getProject() {
        return project;
    }
    
    protected ArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }
    
    protected void init() throws MojoExecutionException, MojoFailureException {
        super.init();
        // Initialize ant helper instance
        ant.setProject(getProject());
    }
    
    protected boolean isReactorMavenProject(Artifact artifact) {
        for (MavenProject p : reactorProjects) {
            if (p.getGroupId().equals(artifact.getGroupId()) && p.getArtifactId().equals(artifact.getArtifactId())
                    && p.getVersion().equals(artifact.getVersion())) {
                return true;
            }
        }
        return false;
    }
    
    protected MavenProject getReactorMavenProject(Artifact artifact) {
        for (MavenProject p : reactorProjects) {
            // Support loose configuration to all sub-module projects in the reactorProjects object. 
            // Need to be able to retrieve all transitive dependencies in these projects.
            if (p.getGroupId().equals(artifact.getGroupId()) && p.getArtifactId().equals(artifact.getArtifactId())
                    && p.getVersion().equals(artifact.getVersion())) {
                p.setArtifactFilter(new ArtifactFilter() {
                    @Override
                    public boolean include(Artifact artifact) {
                        if ("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope())) {
                            return true;
                        }
                        return false;
                    }
                });
                return p;
            }
        }
        
        return null;
    }
    
    //
    // Override methods in org.codehaus.mojo.pluginsupport.MojoSupport to resolve/create Artifact 
    // from ArtifactItem with Maven3 APIs.
    //
    
    /**
     * Resolves the Artifact from the remote repository if necessary. If no version is specified, it will
     * be retrieved from the dependency list or from the DependencyManagement section of the pom.
     *
     *
     * @param item  The item to create an artifact for; must not be null
     * @return      The artifact for the given item
     *
     * @throws MojoExecutionException   Failed to create artifact
     */
//    @Override
//    protected Artifact getArtifact(final ArtifactItem item) throws MojoExecutionException {
//        assert item != null;
//        Artifact artifact = null;
//        
//        if (item.getVersion() != null) {
//            // if version is set in ArtifactItem, it will always override the one in project dependency
//            artifact = createArtifact(item);
//        } else {
//            // Return the artifact from the project dependency if it is available and the mojo
//            // should have requiresDependencyResolution=ResolutionScope.COMPILE_PLUS_RUNTIME set
//            artifact = resolveFromProjectDependencies(item);
//            
//            if (artifact != null) {
//                // in case it is not resolved yet
//                if (!artifact.isResolved()) {
//                    item.setVersion(artifact.getVersion());
//                    artifact = createArtifact(item);
//                }
//            } else if (resolveFromProjectDepMgmt(item) != null) {
//                // if item has no version set, try to get it from the project dependencyManagement section
//                // get version from dependencyManagement
//                item.setVersion(resolveFromProjectDepMgmt(item).getVersion());
//                artifact = createArtifact(item);
//            } else {
//                    throw new MojoExecutionException(
//                            "Unable to find artifact version of " + item.getGroupId() + ":" + item.getArtifactId()
//                                    + " in either project dependencies or in project dependencyManagement.");
//            }
//        }
//        
//        return artifact;
//    }
    
    /**
     * Equivalent to {@link #getArtifact(ArtifactItem)} with an ArtifactItem
     * defined by the given the coordinates.
     * 
     * @param groupId
     *            The group ID
     * @param artifactId
     *            The artifact ID
     * @param type
     *            The type (e.g. jar)
     * @param version
     *            The version, or null to retrieve it from the dependency list
     *            or from the DependencyManagement section of the pom.
     * @return Artifact The artifact for the given item
     * @throws MojoExecutionException
     *             Failed to create artifact
     */
    protected Artifact getArtifact(String groupId, String artifactId, String type, String version) throws MojoExecutionException {
        ArtifactItem item = new ArtifactItem();
        item.setGroupId(groupId);
        item.setArtifactId(artifactId);
        item.setType(type);
        item.setVersion(version);

        return super.getArtifact(item);
    }

    /**
     * Create a new artifact.
     *
     * @param item  The item to create an artifact for
     * @return      A resolved artifact for the given item.
     *
     * @throws MojoExecutionException   Failed to create artifact
     */
//    @Override
//    protected Artifact createArtifact(final ArtifactItem item) throws MojoExecutionException {
//        assert item != null;
//        
//        if (item.getVersion() == null) {
//            throw new MojoExecutionException("Unable to find artifact without version specified: " + item.getGroupId()
//                + ":" + item.getArtifactId() + ":" + item.getVersion() + " in either project dependencies or in project dependencyManagement.");
//        }
//        
////        // if version is a range get the highest available version
////        if (item.getVersion().trim().startsWith("[") || item.getVersion().trim().startsWith("(") ) {
////            item.setVersion(resolveVersionRange(item.getGroupId(), item.getArtifactId(), item.getType(), item.getVersion()));
////        }
//        
//        return super.createArtifact(item);
//    }
    
    private Artifact resolveFromProjectDependencies(ArtifactItem item) {
        Set<Artifact> actifacts = getProject().getArtifacts();
        
        for (Artifact artifact : actifacts) {
            if (artifact.getGroupId().equals(item.getGroupId()) && artifact.getArtifactId().equals(item.getArtifactId())
                    && artifact.getType().equals(item.getType())) {
                log.debug("Found ArtifactItem from project dependencies: " + artifact.getGroupId() + ":"
                        + artifact.getArtifactId() + ":" + artifact.getVersion());
                // if (!artifact.getVersion().equals(item.getVersion())) {
                // item.setVersion(artifact.getVersion());
                // }
                return artifact;
            }
        }
        
        log.debug(item.getGroupId() + ":" + item.getArtifactId() + ":" + item.getVersion()
                + " is not found from project dependencies.");
        return null;
    }
    
    private Dependency resolveFromProjectDepMgmt(ArtifactItem item) {
        // if project has dependencyManagement section
        if (getProject().getDependencyManagement() != null) {
            List<Dependency> list = getProject().getDependencyManagement().getDependencies();
            
            for (Dependency dependency : list) {
                if (dependency.getGroupId().equals(item.getGroupId())
                        && dependency.getArtifactId().equals(item.getArtifactId())
                        && dependency.getType().equals(item.getType())) {
                    log.debug("Found ArtifactItem from project dependencyManagement " + dependency.getGroupId() + ":"
                            + dependency.getArtifactId() + ":" + dependency.getVersion());
                    return dependency;
                }
            }
        }
        log.debug(item.getGroupId() + ":" + item.getArtifactId() + ":" + item.getVersion()
                + " is not found from project dependencyManagement.");
        return null;
    }
    
//    private Artifact resolveArtifactItem(final ArtifactItem item) throws MojoExecutionException {
//        Artifact artifact = new DefaultArtifact(item.getGroupId(), item.getArtifactId(), item.getVersion(),
//                Artifact.SCOPE_PROVIDED, item.getType(), null, new DefaultArtifactHandler("jar"));
//        
//        File artifactFile = resolveArtifactFile(artifact);
//        
//        if (artifactFile != null && artifactFile.exists()) {
//            artifact.setFile(artifactFile);
//            artifact.setResolved(true);
//            log.debug(item.getGroupId() + ":" + item.getArtifactId() + ":" + item.getVersion()
//                    + " is resolved from project repositories.");
//        } else {
//            getLog().warn("Artifact " + item.getGroupId() + ":" + item.getArtifactId() + ":" + item.getVersion()
//                    + " has no attached file.");
//            artifact.setResolved(false);
//        }
//        return artifact;
//    }
    
//    private File resolveArtifactFile(Artifact artifact) throws MojoExecutionException {
//        ArtifactResolutionRequest req = new ArtifactResolutionRequest().setRemoteRepositories(project.getRemoteArtifactRepositories()).setArtifact(artifact);
//        log.info("repo system is set: " + (this.repositorySystem == null));
//        ArtifactResolutionResult resolutionResult = this.repositorySystem.resolve(req);
//        if (!resolutionResult.isSuccess()) {
//            throw new MojoExecutionException("Unable to resolve artifact: " + artifact.getGroupId() + ":"
//                    + artifact.getArtifactId() + ":" + artifact.getVersion());
//        }
//        Set<Artifact> artifacts = resolutionResult.getArtifacts();
//        if (!artifacts.isEmpty()) {
//            File artifactFile = artifacts.toArray(new Artifact[0])[0].getFile();
//            return artifactFile;
//        } else {
//            throw new MojoExecutionException("Unable to resolve artifact: " + artifact.getGroupId() + ":"
//                    + artifact.getArtifactId() + ":" + artifact.getVersion() + ". Empty list of artifact files.");
//        }
//    }

//    private String resolveVersionRange(String groupId, String artifactId, String extension, String version) throws MojoExecutionException {
//        try {
//            List<Restriction> restrictions = VersionRange.createFromVersionSpec(version).getRestrictions();
//            if (!restrictions.isEmpty()) {
//                return restrictions.get(0).getUpperBound().toString();
//            } else {
//                throw new MojoExecutionException("Could not get the highest version from the range: " + version);
//            }
//        } catch (InvalidVersionSpecificationException e) {
//            throw new MojoExecutionException("Unable to resolve version range from " + groupId + ":" + artifactId + ":" + extension + ":" + version + ".", e);
//        }
//    }
}
