/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.shell;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.cooder.mos.MosSystem;
import org.cooder.mos.Utils;
import org.cooder.mos.fs.FileDescriptor;
import org.cooder.mos.fs.IFileSystem;
import org.cooder.mos.shell.command.*;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

@Command(name = "",
        subcommands = {HelpCommand.class, Mkdir.class, ListCommand.class, Cat.class, Echo.class, Pwd.class,
                Remove.class, Touch.class})
public class Shell implements Runnable {

    private FileDescriptor current;
    public InputStream in;
    public PrintStream out;
    public PrintStream err;
    private Terminal terminal;
    private ExitCallback callback;
    private ChannelSession channel;
    private Environment env;

    Shell(InputStream in, OutputStream out, OutputStream err, ExitCallback callback, ChannelSession channel, Environment env) throws IOException {
        this.current = MosSystem.fileSystem().find(new String[]{"/"});
        this.terminal = TerminalBuilder.builder().streams(in, out).build();
        this.in = terminal.input();
        this.out = new PrintStream(terminal.output());
        this.err = new PrintStream(terminal.output());
        this.callback = callback;
        this.channel = channel;
        this.env = env;
    }

    public String currentPath() {
        return current.isRoot() ? current.getName() : current.getPath();
    }

    private void loop() {
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        try {
            while (true) {
                String cmd = reader.readLine(this.prompt());
                if ("exit".equalsIgnoreCase(cmd) || "quit".equalsIgnoreCase(cmd)) {
//                    out.println("bye~");
                    break;
                } else if (cmd.length() == 0) {
                    continue;
                }

                try {
                    String[] as = Utils.parseArgs(cmd);
                    new ShellCommandLine(this, this.err).execute(as);
                } catch (Exception e) {
                    err.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            callback.onExit(0, "byebye");
        }
    }

    @Command(name = "format", hidden = true)
    public void format() throws IOException {
        MosSystem.fileSystem().format();
        current = MosSystem.fileSystem().find(new String[]{"/"});
        out.println("disk format success.");
    }

    @Command(name = "cd")
    public void cd(@Parameters(paramLabel = "<path>") String path) {
        String[] paths = null;

        if (path.equals("/")) {
            current = MosSystem.fileSystem().find(new String[]{"/"});
            return;
        }

        if (path.equals("..")) {
            if (current.isRoot()) {
                return;
            }
            String[] ps = Utils.normalizePath(current.getParentPath());
            current = MosSystem.fileSystem().find(ps);
            return;
        }

        paths = absolutePath(path);
        FileDescriptor node = MosSystem.fileSystem().find(paths);
        if (node == null) {
            out.println(path + ": No such file or directory");
            return;
        }

        if (!node.isDir()) {
            out.println(path + ": Not a directory");
            return;
        }

        current = node;
    }

    public String[] absolutePath(String path) {
        if (path.equals(".")) {
            path = current.getPath();
        } else if (path.equals("..")) {
            if (current.isRoot()) {
                path = current.getPath();
            } else {
                path = current.getParentPath();
            }
        } else if (!path.startsWith("/")) {
            path = current.getPath() + IFileSystem.separator + path;
        }
        return Utils.normalizePath(path);
    }

    @Override
    public void run() {
        this.loop();
    }

    private String prompt() {
        return String.format("root@" + env.getEnv().get("USER") + ":%s$ ", currentPath());
    }
}
