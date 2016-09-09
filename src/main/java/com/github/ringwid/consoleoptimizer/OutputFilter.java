package com.github.ringwid.consoleoptimizer;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

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
                Plugin plugin = this.plugin.getServer().getPluginManager().getPlugin(interceptPlugin);
                if (plugin == null) {
                    continue;
                }
                String[] split = element.getClassName().split("\\.");
                split = Arrays.copyOfRange(split, 0, split.length - 1);
                StringBuilder packageName = new StringBuilder();
                for (String s : split) {
                    packageName = packageName.append(s + ".");
                }
                if (packageName.toString().startsWith(plugin.getClass().getPackage().getName())) {
                    if (!this.plugin.getPluginLog().containsKey(interceptPlugin)) {
                        this.plugin.getPluginLog().put(interceptPlugin, new ArrayList<>());
                    }
                    this.plugin.getPluginLog().get(interceptPlugin).add(str);
                    match = true;
                    break loop;
                }
            }
        }
        println(String.valueOf(match));
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
