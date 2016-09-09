package com.github.ringwid.consoleoptimizer;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Writer;

import static com.github.ringwid.consoleoptimizer.ConsoleOptimizer.PREFIX;

/**
 * Project ConsoleOptimizer
 */
public class ErrorOutputFilter extends OutputFilter {

    public long lastNotify;

    public ErrorOutputFilter(ConsoleOptimizer consoleOptimizer, Writer backup) {
        super(consoleOptimizer, backup);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String str = String.valueOf(cbuf).substring(off, off + len);
        plugin.getErrorLog().add(str);
        str = str.replaceAll("\u001B\\[[;\\d]*m", "");
        str = str.replaceAll(" ", "");
        String packageName = getErrorPackageName(str);
        lastNotify = System.currentTimeMillis();
        if (System.currentTimeMillis() - lastNotify < 5000) {
            return;
        }
        if (plugin.getConfig().getBoolean("notify") && packageName != null) {
            for (Plugin plugin1 : plugin.getServer().getPluginManager().getPlugins()) {
                if (packageName.startsWith(plugin1.getClass().getPackage().getName())) {
                    plugin.getColoredLogger().sendMessage(PREFIX + "The plugin " + plugin1.getName() + " generates an error.");
                    return;
                }
            }
            plugin.getColoredLogger().sendMessage(PREFIX + "The server generates an error.");
        }
    }

    private String getErrorPackageName(String str) {
        if (!(str.contains("at") && str.contains("("))) {
            return null;
        }
        str = str.substring(str.indexOf("at") + 2, str.indexOf("("));
        return str;
    }

}
