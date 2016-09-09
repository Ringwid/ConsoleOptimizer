package com.github.ringwid.consoleoptimizer;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Project ConsoleOptimizer
 */
public class OutputFilter extends Writer {

    protected ConsoleOptimizer plugin;
    protected Writer out;

    public OutputFilter(ConsoleOptimizer consoleOptimizer, Writer backup) {
        plugin = consoleOptimizer;
        this.out = backup;
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        String str = String.valueOf(cbuf).substring(off, off + len);
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        boolean match = false;

        loop:
        for (StackTraceElement element : elements) {
            for (String interceptPlugin : plugin.getConfig().getStringList("interceptPluginList")) {
                if (element.getClassName().contains("consoleoptimizer")) {
                    continue;
                }
                Plugin plugin = this.plugin.getServer().getPluginManager().getPlugin(interceptPlugin);
                if (plugin == null) {
                    continue;
                }
                if (element.getClassName().contains(plugin.getClass().getPackage().getName())) {
                    if (!this.plugin.getPluginLog().containsKey(interceptPlugin)) {
                        this.plugin.getPluginLog().put(interceptPlugin, new ArrayList<>());
                    }
                    this.plugin.getPluginLog().get(interceptPlugin).add(str);
                    match = true;
                    break loop;
                }
            }
        }
        if (!match) {
            print(str);
        }
    }

    protected void println(String s) {
        print("\n" + s);
    }

    protected void print(String str) {
        try {
            out.write(str.toCharArray(), 0, str.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }

}
