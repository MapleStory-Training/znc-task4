package org.cooder.mos.shell;

import org.apache.sshd.common.SshException;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.scp.common.ScpSourceStreamResolver;
import org.apache.sshd.scp.common.ScpTargetStreamResolver;
import org.apache.sshd.scp.common.helpers.DefaultScpFileOpener;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;
import org.cooder.mos.MosSystem;
import org.cooder.mos.Utils;
import org.cooder.mos.api.FileInputStream;
import org.cooder.mos.api.FileOutputStream;
import org.cooder.mos.api.MosFile;
import org.cooder.mos.fs.FileDescriptor;
import org.cooder.mos.fs.FileSystem;
import org.cooder.mos.shell.file.MosDirectoryStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author zhengnachuan
 * @date 2021-07-05
 * @description
 */
public class MosScpFileOpener extends DefaultScpFileOpener {

    @Override
    public Path resolveIncomingReceiveLocation(Session session, Path path, boolean recursive, boolean shouldBeDir, boolean preserve) throws IOException {
        if (!shouldBeDir) {
            return path;
        }
        String[] paths = Utils.normalizePath(path.toString());
        FileDescriptor fd = MosSystem.fileSystem().find(paths);
        if (fd == null) {
            throw new SshException("Target directory " + path + " does not exist");
        }
        return path;
    }

    @Override
    public Path resolveIncomingFilePath(Session session, Path localPath, String name, boolean preserve, Set<PosixFilePermission> permissions, ScpTimestampCommandDetails time) throws IOException {

        boolean status = this.checkFileExists(localPath);

        Path file = null;
        if (status && this.isDir(localPath)) {
            String localName = name.replace('/', File.separatorChar);
            file = localPath.resolve(localName);
        } else if (!status) {
            Path parent = localPath.getParent();

            status = this.checkFileExists(parent);
            if (status && this.isDir(parent)) {
                file = localPath;
            }
        }

        if (file == null) {
            throw new IOException("Cannot write to " + localPath);
        }

        status = this.checkFileExists(file);

        if (!(status && this.isDir(file))) {
            String[] paths = Utils.normalizePath(file.toString());
            MosFile mosFile = new MosFile(paths);
            if (!mosFile.mkdir()) {
                throw new IOException("mkdir: " + name + ": No such file or directory");
            }
        }

        return file;
    }

    private boolean checkFileExists(Path path) {
        String[] paths = Utils.normalizePath(path.toString());
        FileDescriptor fd = MosSystem.fileSystem().find(paths);
        return fd != null;
    }

    @Override
    public Path resolveOutgoingFilePath(Session session, Path localPath, LinkOption... options) throws IOException {
        String[] paths = Utils.normalizePath(localPath.toString());
        FileDescriptor fd = MosSystem.fileSystem().find(paths);
        if (fd == null) {
            throw new IOException(localPath + ": no such file or directory");
        }
        return localPath;
    }

    @Override
    public boolean sendAsRegularFile(Session session, Path path, LinkOption... options) {
        return !this.isDir(path);
    }

    public boolean isDir(Path path) {
        String[] paths = Utils.normalizePath(path.toString());
        FileDescriptor fd = MosSystem.fileSystem().find(paths);
        return fd.isDir();
    }

    @Override
    public boolean sendAsDirectory(Session session, Path path, LinkOption... options) {
        return this.isDir(path);
    }

    @Override
    public DirectoryStream<Path> getLocalFolderChildren(Session session, Path path) {
        return new MosDirectoryStream(path);
    }

    @Override
    public Set<PosixFilePermission> getLocalFilePermissions(Session session, Path path, LinkOption... options) {

        Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.OTHERS_READ);
        return perms;
    }

    @Override
    public ScpSourceStreamResolver createScpSourceStreamResolver(Session session, Path path) {
        return new MosScpSourceStreamResolver(path, this);
    }

    @Override
    public ScpTargetStreamResolver createScpTargetStreamResolver(Session session, Path path) {
        return new MosScpTargetStreamResolver(path, this);
    }

    @Override
    public InputStream openRead(Session session, Path file, long size, Set<PosixFilePermission> permissions, OpenOption... options)
            throws IOException {
        String[] paths = Utils.normalizePath(file.toString());
        return new FileInputStream(new MosFile(paths));
    }

    @Override
    public OutputStream openWrite(Session session, Path file, long size, Set<PosixFilePermission> permissions, OpenOption... options)
            throws IOException {
        String[] paths = Utils.normalizePath(file.toString());
        return new FileOutputStream(new MosFile(paths), FileSystem.WRITE);
    }
}