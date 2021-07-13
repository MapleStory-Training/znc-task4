package org.cooder.mos.shell.file;

import com.google.common.collect.Lists;
import org.cooder.mos.Utils;
import org.cooder.mos.api.MosFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * @author zhengnachuan
 * @date 2021-07-10
 * @description
 */
public class MosPathIterator implements Iterator<Path> {

    private List<Path> childPathList;
    private int curOffset;

    MosPathIterator(Path path) {
        childPathList = this.listChild(Objects.requireNonNull(path, "No root path provided"));
        curOffset = 0;
    }

    private List<Path> listChild(Path path) {
        String[] paths = Utils.normalizePath(path.toString());
        MosFile file = new MosFile(paths);
        MosFile[] files = file.listFiles();
        List<Path> result = Lists.newArrayList();
        if (files.length == 0) {
            return result;
        }
        for (MosFile mosFile : files) {
            result.add(Paths.get(mosFile.getAbsolutePath()));
        }
        return result;
    }

    @Override
    public boolean hasNext() {
        return curOffset != childPathList.size();
    }

    @Override
    public Path next() {
        if (curOffset == childPathList.size()) {
            throw new NoSuchElementException("No next entry");
        }
        return childPathList.get(curOffset++);
    }
}
