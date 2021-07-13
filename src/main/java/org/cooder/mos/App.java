/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos;


import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.cooder.mos.device.FileDisk;
import org.cooder.mos.shell.MosScpFileOpener;
import org.cooder.mos.shell.ShellFactoryImpl;

import java.io.File;
import java.io.IOException;

public class App {
    public static  void main(String[] args) throws IOException {

        MosSystem.fileSystem().bootstrap(new FileDisk("mos-disk"));
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(22);

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("/Users/zhengnachuan/GitLab/MapleStory-Training/mos/ssh").toPath()));
        sshd.setPasswordAuthenticator((username, password, serverSession) -> password.equals("mos"));
        sshd.setShellFactory(ShellFactoryImpl.INSTANCE);

        ScpCommandFactory factory = new ScpCommandFactory.Builder()
                .withFileOpener(new MosScpFileOpener())
//                .withDelegate()
//                .withDelegateShellFactory()
                .build();
        sshd.setCommandFactory(factory);

        try {
            sshd.start();
        } finally {

        }
        //保持java进程不关闭
        Object obj = new Object();
        synchronized (obj) {
            try {
                obj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        MosSystem.fileSystem().shutdown();
    }
}
