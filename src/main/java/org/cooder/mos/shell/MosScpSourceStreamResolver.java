package org.cooder.mos.shell;

import com.google.common.collect.Sets;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.logging.AbstractLoggingBean;
import org.apache.sshd.scp.common.ScpFileOpener;
import org.apache.sshd.scp.common.ScpSourceStreamResolver;
import org.apache.sshd.scp.common.helpers.DefaultScpFileOpener;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;
import org.cooder.mos.MosSystem;
import org.cooder.mos.Utils;
import org.cooder.mos.fs.FileDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhengnachuan
 * @date 2021-07-04
 * @description
 */
public class MosScpSourceStreamResolver extends AbstractLoggingBean implements ScpSourceStreamResolver {

    protected final Path path;
    private final ScpFileOpener opener;
    protected final Path name;
    private final Set<PosixFilePermission> perms;
    private final long size;
    private final ScpTimestampCommandDetails time;

    MosScpSourceStreamResolver(Path path, ScpFileOpener opener) {

        this.path = Objects.requireNonNull(path, "No path specified");
        this.opener = (opener == null) ? DefaultScpFileOpener.INSTANCE : opener;
        this.name = path.getFileName();
        this.perms = Sets.newHashSet(PosixFilePermission.OWNER_READ);

        String[] paths = Utils.normalizePath(path.toString());
        FileDescriptor fd = MosSystem.fileSystem().find(paths);
        this.size = fd.getFileSize();
        this.time = new ScpTimestampCommandDetails(fd.getWriteTime(), fd.getWriteTime());
    }

    @Override
    public String getFileName() {
        return name.toString();
    }

    @Override
    public Collection<PosixFilePermission> getPermissions() {
        return perms;
    }

    @Override
    public ScpTimestampCommandDetails getTimestamp() {
        return time;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public Path getEventListenerFilePath() {
        return path;
    }

    @Override
    public InputStream resolveSourceStream(Session session, long length, Set<PosixFilePermission> permissions, OpenOption... options)
            throws IOException {
        return opener.openRead(session, getEventListenerFilePath(), length, permissions, options);
    }

    @Override
    public void closeSourceStream(Session session, long length, Set<PosixFilePermission> permissions, InputStream stream)
            throws IOException {
        opener.closeRead(session, getEventListenerFilePath(), length, permissions, stream);
    }

    @Override
    public String toString() {
        return String.valueOf(getEventListenerFilePath());
    }
}
