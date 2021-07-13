package org.cooder.mos.shell;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author zhengnachuan
 * @date 2021-06-04
 * @description
 */
public class ShellCommandImpl implements Command {

    private InputStream in;
    public OutputStream out;
    private OutputStream err;
    private ExitCallback callback;

    ShellCommandImpl() {
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(ChannelSession channelSession, Environment environment) throws IOException {
        Shell shell = new Shell(in, out, err, callback, channelSession, environment);
        new Thread(shell).start();

    }

    @Override
    public void destroy(ChannelSession channelSession) {

    }
}
