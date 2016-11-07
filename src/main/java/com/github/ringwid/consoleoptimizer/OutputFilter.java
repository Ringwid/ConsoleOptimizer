package com.github.ringwid.consoleoptimizer;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Project ConsoleOptimizer
 */
public class OutputFilter extends Writer implements Filter {

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

        plugin:
        for (StackTraceElement element : elements) {
            for (String interceptPlugin : plugin.getConfig().getStringList("interceptPluginList")) {
                if (element.getClassName().contains("consoleoptimizer")) {
                    continue;
                }
                Plugin plugin = this.plugin.getServer().getPluginManager().getPlugin(interceptPlugin);
                if (plugin == null) {
                    continue;
                }

                try {
                    if (findPathJar(Class.forName(element.getClassName())).equals(findPathJar(plugin.getClass()))) {
                        if (!this.plugin.getPluginLog().containsKey(interceptPlugin)) {
                            this.plugin.getPluginLog().put(interceptPlugin, new ArrayList<>());
                        }
                        this.plugin.getPluginLog().get(interceptPlugin).add(str);
                        match = true;
                        break plugin;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        for (String interceptKeyword : plugin.getConfig().getStringList("interceptKeywordList")) {
            if (str.toLowerCase().contains(interceptKeyword.toLowerCase())) {
                keyword:
                for (Plugin plugin1 : plugin.getServer().getPluginManager().getPlugins()) {
                    for (StackTraceElement element : elements) {
                        if (element.getClassName().contains("consoleoptimizer")) {
                            continue;
                        }

                        try {
                            if (findPathJar(Class.forName(element.getClassName())).equals(findPathJar(plugin1.getClass()))) {
                                if (!this.plugin.getPluginLog().containsKey(plugin1.getName())) {
                                    this.plugin.getPluginLog().put(plugin1.getName(), new ArrayList<>());
                                }
                                this.plugin.getPluginLog().get(plugin1.getName()).add(str);
                                match = true;
                                break keyword;
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
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

    public static String findPathJar(Class<?> context) {
        String rawName = context.getName();
        String classFileName;
    /* rawName is something like package.name.ContainingClass$ClassName. We need to turn this into ContainingClass$ClassName.class. */ {
            int idx = rawName.lastIndexOf('.');
            classFileName = (idx == -1 ? rawName : rawName.substring(idx+1)) + ".class";
        }

        String uri = context.getResource(classFileName).toString();
        if (uri.startsWith("file:")) throw new IllegalStateException("This class has been loaded from a directory and not from a jar file.");
        if (!uri.startsWith("jar:file:")) {
            int idx = uri.indexOf(':');
            String protocol = idx == -1 ? "(unknown)" : uri.substring(0, idx);
            throw new IllegalStateException("This class has been loaded remotely via the " + protocol +
                    " protocol. Only loading from a jar on the local file system is supported.");
        }

        int idx = uri.indexOf('!');
        //As far as I know, the if statement below can't ever trigger, so it's more of a sanity check thing.
        if (idx == -1) throw new IllegalStateException("You appear to have loaded this class from a local jar file, but I can't make sense of the URL!");

        try {
            String fileName = URLDecoder.decode(uri.substring("jar:file:".length(), idx), Charset.defaultCharset().name());
            return new File(fileName).getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("default charset doesn't exist. Your VM is borked.");
        }
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        String msg = record.getMessage();
        char[] chars = new char[msg.length()];
        for (int i = 0; i < msg.getBytes().length; i++) {
            chars[i] = (char) msg.getBytes()[i];
        }
        try {
            write(chars, 0, chars.length);
        } catch (IOException ignored) {

        }
        return false;
    }
}
