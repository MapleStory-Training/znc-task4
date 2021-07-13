/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.fs.fat16;

import org.cooder.mos.fs.IFileSystem;
import org.cooder.mos.fs.fat16.Layout.DirectoryEntry;

import java.util.Arrays;

public class DirectoryTreeNode {
    private DirectoryEntry entry;
    public final DirectoryTreeNode parent;
    private DirectoryTreeNode[] children;
    private int sectorIdx = -1;
    private int sectorOffset = -1;
    private boolean fold = true;

    DirectoryTreeNode(DirectoryTreeNode parent, DirectoryEntry entry) {
        this.parent = parent;
        this.entry = entry;
    }

    public DirectoryTreeNode[] getChildren() {
        return children;
    }

    void setChildren(DirectoryTreeNode[] children) {
        this.children = children;
    }

    int getSectorIdx() {
        return sectorIdx;
    }

    void setSectorIdx(int sectorIdx) {
        this.sectorIdx = sectorIdx;
    }

    int getSectorOffset() {
        return sectorOffset;
    }

    void setSectorOffset(int sectorOffset) {
        this.sectorOffset = sectorOffset;
    }

    public DirectoryEntry getEntry() {
        return entry;
    }

    public boolean isDir() {
        return isRoot() || ((entry.attrs & DirectoryEntry.ATTR_MASK_DIR) != 0);
    }

    public boolean isRoot() {
        return entry == null;
    }

    boolean isFold() {
        return fold;
    }

    void unfold() {
        this.fold = false;
    }

    public void fold() {
        this.fold = true;
    }

    public String getName() {
        if (isRoot()) {
            return "/";
        } else {
            return byteArray2String(entry.fileName);
        }
    }

    public String getPath() {
        if (isRoot()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            sb.append(parent.getPath()).append(IFileSystem.separator);
        }
        sb.append(getName());
        return sb.toString();
    }

    DirectoryTreeNode find(String name) {
        if (!isDir()) {
            throw new IllegalArgumentException();
        }

        if (isFold()) {
            throw new IllegalStateException();
        }

        if (children == null) {
            return null;
        }

        DirectoryTreeNode node = null;
        for (DirectoryTreeNode child : children) {
            node = child;
            if (nameEquals(node.entry, name)) {
                return node;
            }
        }

        return null;
    }

    private static boolean nameEquals(DirectoryEntry entry, String fileName) {
        return Arrays.equals(entry.fileName, string2ByteArray(fileName, DirectoryEntry.FILE_NAME_LENGTH));
    }

    private static byte[] string2ByteArray(String name, int length) {
        byte[] b1 = name.getBytes();
        byte[] b2 = new byte[length];
        System.arraycopy(b1, 0, b2, 0, Math.min(b1.length, length));
        return b2;
    }

    private static String byteArray2String(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte value : b) {
            if (value == 0) {
                break;
            }
            sb.append((char) (value & 0xFF));
        }
        return sb.toString();
    }

    DirectoryTreeNode create(String name, boolean isDir) {
        DirectoryTreeNode node = nextFreeNode();
        DirectoryEntry entry = node.entry;

        byte[] b = string2ByteArray(name, DirectoryEntry.FILE_NAME_LENGTH);
        System.arraycopy(b, 0, entry.fileName, 0, b.length);
        entry.attrs |= isDir ? DirectoryEntry.ATTR_MASK_DIR : 0;
        node.setWriteTime(System.currentTimeMillis());

        return node;
    }

    DirectoryTreeNode firstTreeNode() {
        if (!isDir()) {
            throw new IllegalArgumentException();
        }

        if (isFold()) {
            throw new IllegalStateException();
        }

        if (children == null) {
            return null;
        }

        DirectoryTreeNode node = null;
        for (DirectoryTreeNode child : children) {
            node = child;
            if (!node.isFree()) {
                return node;
            }
        }
        return null;
    }

    private DirectoryTreeNode nextFreeNode() {
        if (!isDir()) {
            throw new IllegalArgumentException();
        }

        if (isFold()) {
            throw new IllegalStateException();
        }

        if (children == null) {
            return null;
        }

        DirectoryTreeNode node = null;
        for (DirectoryTreeNode child : children) {
            node = child;
            if (node.isFree()) {
                return node;
            }
        }
        return null;
    }

    private boolean isFree() {
        return entry.fileName[0] == 0;
    }

    public boolean valid() {
        return entry != null && !isFree();
    }

    void reset() {
        this.fold = true;
        this.entry = new DirectoryEntry();
        this.children = null;
    }

    public void setFileSize(int fileSize) {
        this.entry.fileSize = fileSize;
    }

    void setWriteTime(long currentTimeMillis) {
        int sec = (int) (currentTimeMillis / 1000);
        this.entry.lastWriteTime = (short) (sec & 0xFFFF);
        this.entry.lastWriteDate = (short) (sec >>> 16 & 0xFFFF);
    }

    public long getWriteTime() {
        long sec = this.entry.lastWriteDate;
        sec = sec << 16;
        sec = sec | (0xFFFF & this.entry.lastWriteTime);
        return sec * 1000;
    }

    int getFileSize() {
        return entry.fileSize;
    }
}
