package org.cooder.mos.shell;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

/**
 * @author zhengnachuan
 * @date 2021-06-02
 * @description
 */
public class ShellFactoryImpl implements ShellFactory {

    public static final ShellFactoryImpl INSTANCE = new ShellFactoryImpl();

    @Override
    public Command createShell(ChannelSession channelSession) {
        return new ShellCommandImpl();
    }

}
