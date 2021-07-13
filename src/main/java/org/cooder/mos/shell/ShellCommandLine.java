package org.cooder.mos.shell;

import picocli.CommandLine;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author zhengnachuan
 * @date 2021-06-09
 * @description
 */
public class ShellCommandLine extends CommandLine {

    private PrintWriter err;

    ShellCommandLine(Object command, PrintStream err) {
        super(command);
        this.err = new PrintWriter(err);
    }

    @Override
    public CommandLine setErr(PrintWriter err) {
        this.err = err;
        for (CommandLine sub : getSubcommands().values()) {
            sub.setErr(err);
        }
        return this;
    }

    @Override
    public PrintWriter getErr() {
        setErr(new PrintWriter(this.err, true));
        return err;
    }
}
