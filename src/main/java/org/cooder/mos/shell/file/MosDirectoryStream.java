package org.cooder.mos.shell.file;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;

/**
 * @author zhengnachuan
 * @date 2021-07-10
 * @description
 */
public class MosDirectoryStream implements DirectoryStream<Path> {

    private MosPathIterator pathIterator;
    private final Path path;

    public MosDirectoryStream(Path path) {
        this.path = Objects.requireNonNull(path, "No path specified");
        pathIterator = new MosPathIterator(this.getRootPath());
    }

    private Path getRootPath() {
        return path;
    }

    @Override
    public Iterator<Path> iterator() {
        Iterator<Path> iter = pathIterator;
        pathIterator = null;
        return iter;
    }

    @Override
    public void close() {

    }
}
