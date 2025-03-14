package org.p2proto.ddl;

import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.resource.ResourceAccessor;
import liquibase.resource.Resource;
import liquibase.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * In-memory ResourceAccessor for Liquibase serving exactly one 'file' (changelog) from a String.
 */
public class InMemoryResourceAccessor implements ResourceAccessor {

    private final String inMemoryPath;
    private final String changelogContent;

    /**
     * @param inMemoryPath    The path Liquibase will look for (e.g. "in-memory-changelog.json")
     * @param changelogContent The actual changelog text (JSON/YAML/XML) in-memory
     */
    public InMemoryResourceAccessor(String inMemoryPath, String changelogContent) {
        this.inMemoryPath = inMemoryPath;
        this.changelogContent = changelogContent;
    }

    /**
     * Required by ResourceAccessor:
     * Returns a List of all Resources matching the given path.
     * We only serve one in-memory resource, so return it if path matches.
     */
    @Override
    public List<Resource> getAll(String path) throws IOException {
        if (inMemoryPath.equals(path) && !StringUtil.isEmpty(changelogContent)) {
            // Return a single Resource that opens a stream over our changelogContent
            return Collections.singletonList(new InMemoryResource(path, changelogContent, this));
        }
        return Collections.emptyList();
    }

    /**
     * Required by ResourceAccessor:
     * Returns a List of all Resources under 'path' if searching recursively.
     * We'll treat this the same as getAll(...) to keep it simple.
     */
    @Override
    public List<Resource> search(String path, boolean recursive) throws IOException {
        return getAll(path);
    }

    /**
     * Required by ResourceAccessor:
     * Describes where this accessor looks for resources (for logging/debugging).
     */
    @Override
    public List<String> describeLocations() {
        return Collections.singletonList("InMemory");
    }

    /**
     * Because ResourceAccessor extends AutoCloseable,
     * we must implement 'close()' even if we don't use it.
     */
    @Override
    public void close() throws IOException {
        // No resources to close in memory
    }

    /*
     * ========== Internal class for our single in-memory resource ==========
     */
    private static class InMemoryResource extends liquibase.resource.AbstractResource {

        private final String content;
        private final ResourceAccessor parentAccessor;

        InMemoryResource(String path, String content, ResourceAccessor parentAccessor) {
            // Construct a URI. Could be "inmem:/..."
            super(path, URI.create("inmem:" + path.replace(" ", "%20")));
            this.content = content;
            this.parentAccessor = parentAccessor;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(content.getBytes());
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public Resource resolve(String other) {
            try {
                return parentAccessor.get(resolvePath(other));
            } catch (IOException e) {
                throw new UnexpectedLiquibaseException(e);
            }
        }

        @Override
        public Resource resolveSibling(String other) {
            try {
                return parentAccessor.get(resolveSiblingPath(other));
            } catch (IOException e) {
                throw new UnexpectedLiquibaseException(e);
            }
        }

        @Override
        public OutputStream openOutputStream(boolean createIfNeeded) {
            throw new UnexpectedLiquibaseException("Cannot write to an in-memory resource");
        }
    }
}
