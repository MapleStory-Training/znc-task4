package org.cooder.mos.shell;

import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.scp.common.ScpFileOpener;
import org.apache.sshd.scp.common.ScpTargetStreamResolver;
import org.apache.sshd.scp.common.helpers.DefaultScpFileOpener;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;
import org.cooder.mos.MosSystem;
import org.cooder.mos.Utils;
import org.cooder.mos.fs.FileDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhengnachuan
 * @date 2021-07-05
 * @description
 */
public class MosScpTargetStreamResolver implements ScpTargetStreamResolver {

    protected final Path path;
    private final ScpFileOpener opener;
    private final boolean status;
    private Path file;

    MosScpTargetStreamResolver(Path path, ScpFileOpener opener) {
        this.status = this.checkFileExists(path);
        this.path = Objects.requireNonNull(path, "No path specified");
        this.opener = (opener == null) ? DefaultScpFileOpener.INSTANCE : opener;
    }

    private boolean checkFileExists(Path path) {
        String[] paths = Utils.normalizePath(path.toString());
        FileDescriptor fd = MosSystem.fileSystem().find(paths);
        return fd != null;
    }

    @Override
    public OutputStream resolveTargetStream(Session session, String name, long length, Set<PosixFilePermission> perms, OpenOption... options) throws IOException {

        if (file != null) {
            throw new StreamCorruptedException("resolveTargetStream(" + name + ")[" + perms + "] already resolved: " + file);
        }

        LinkOption[] linkOptions = IoUtils.getLinkOptions(true);
        if (status && opener.sendAsDirectory(null, path, linkOptions)) {
            file = Paths.get(name.replace('/', File.separatorChar));
        } else if (status && opener.sendAsRegularFile(null, path, linkOptions)) {
            file = path;
        }
        if (file == null) {
            throw new IOException("Can not write to " + path);
        }

        boolean fileStatus = this.checkFileExists(file);

        if (fileStatus) {
            if (opener.sendAsDirectory(null, file, linkOptions)) {
                throw new IOException("File is a directory: " + file);
            }
        }
        return opener.openWrite(session, file, length, perms, options);
    }

    @Override
    public Path getEventListenerFilePath() {
        if (file == null) {
            return path;
        } else {
            return file;
        }
    }

    @Override
    public void postProcessReceivedData(String name, boolean preserve, Set<PosixFilePermission> perms, ScpTimestampCommandDetails time) {

    }
}
