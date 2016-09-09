package com.github.ringwid.consoleoptimizer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Project ConsoleOptimizer
 */
public class ConsoleOptimizer extends JavaPlugin implements Listener {

    public static final String PREFIX = ChatColor.DARK_BLUE + "[" + ChatColor.BLUE + "ConsoleOptimizer" + ChatColor.DARK_BLUE + "] " + ChatColor.AQUA;

    private Writer outStreamBackup;
    private Writer errStreamBackup;
    private FileConfiguration config;

    private HashMap<String, List<String>> pluginLog = new LinkedHashMap<>();
    private List<String> errorLog = new LinkedList<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Please execute this command on your console.");
        }
        if (args.length < 1) {
            return false;
        }
        String subCommand = args[0];
        switch (subCommand) {
            case "help":
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "------------ " + ChatColor.YELLOW + "ConsoleOptimizer Command List: " + ChatColor.LIGHT_PURPLE + "------------");
                sender.sendMessage(ChatColor.AQUA + "- help:" + ChatColor.GRAY + ChatColor.ITALIC + " Show the help page");
                sender.sendMessage(ChatColor.AQUA + "- filter add <plugin>:" + ChatColor.GRAY + ChatColor.ITALIC + " Add a filter of the specified plugin");
                sender.sendMessage(ChatColor.AQUA + "- filter remove <plugin>:" + ChatColor.GRAY + ChatColor.ITALIC + " Remove the filter of the specified plugin");
                sender.sendMessage(ChatColor.AQUA + "- filter add errors:" + ChatColor.GRAY + ChatColor.ITALIC + " Add the error filter");
                sender.sendMessage(ChatColor.AQUA + "- filter remove errors:" + ChatColor.GRAY + ChatColor.ITALIC + " Remove the error filter");
                sender.sendMessage(ChatColor.AQUA + "- showlog <plugin>:" + ChatColor.GRAY + ChatColor.ITALIC + " Show all the messages intercepted by ConsoleOptimizer");
                sender.sendMessage(ChatColor.AQUA + "- showerrors:" + ChatColor.GRAY + ChatColor.ITALIC + " Show all the errors intercepted by ConsoleOptimizer");
                break;
            case "filter":
                if (args.length < 3) {
                    sender.sendMessage("Usage: /co filter add <pluginName/errors>");
                    break;
                }

                String action = args[1];
                String pluginName = args[2];
                if (!(action.equals("add") || action.equals("remove"))) {
                    sender.sendMessage("Usage: /co filter add <pluginName/errors>");
                }
                if (pluginName.equals("ConsoleOptimizer")) {
                    sender.sendMessage(PREFIX + "Nope!");
                    break;
                } else {
                    if (!pluginName.equals("errors")) {
                        boolean match = false;
                        for (Plugin plugin : getServer().getPluginManager().getPlugins()) {
                            if (plugin.getName().equals(pluginName)) {
                                match = true;
                            }
                        }
                        if (!match) {
                            sender.sendMessage(PREFIX + "The plugin you have entered doesn't exists!");
                            break;
                        }

                        List<String> list = config.getStringList("interceptPluginList");
                        if (action.equals("add")) {
                            if (list.contains(pluginName)) {
                                sender.sendMessage(PREFIX + "The plugin's message is already intercepted.");
                            } else {
                                list.add(pluginName);
                                sender.sendMessage(PREFIX + "Succeed.");
                            }
                        } else {
                            if (!list.contains(pluginName)) {
                                sender.sendMessage(PREFIX + "The plugin's message isn't intercepted.");
                            } else {
                                list.remove(pluginName);
                                sender.sendMessage(PREFIX + "Succeed.");
                            }
                        }
                        config.set("interceptPluginList", list);
                    } else {
                        config.set("interceptErrors", action.equals("add"));
                        if (action.equals("add")) {
                            interceptErrors(true);
                        } else {
                            interceptErrors(false);
                        }
                        sender.sendMessage(PREFIX + "Succeed.");
                    }

                    saveConfig();
                }
                break;
            case "showlog":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /co showlog <pluginName>");
                    break;
                }
                pluginName = args[1];
                if (pluginLog.get(pluginName) == null) {
                    sender.sendMessage(PREFIX + "The plugin " + pluginName + " has no log.");
                    break;
                }
                pluginLog.get(pluginName).forEach(sender::sendMessage);
                break;
            case "showerrors":
                if (errStreamBackup == null) {
                    sender.sendMessage(PREFIX + "Error interception is disabled.");
                }
                if (errorLog.size() == 0) {
                    sender.sendMessage(PREFIX + "There are no errors.");
                    break;
                }
                errorLog.forEach(sender::sendMessage);
                break;
            default:
                return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandExecute(ServerCommandEvent event) {
        if (event.getCommand().startsWith("co") && event.getSender() instanceof ConsoleCommandSender) {
            event.setCancelled(true);
            String[] cmd = event.getCommand().split(" ");
            String[] args = cmd.length > 0 ? Arrays.copyOfRange(cmd, 1, cmd.length) : new String[0];
            if (!onCommand(event.getSender(), getCommand("consoleoptimizer"), cmd[0], args)) {
                System.out.println(getCommand("consoleoptimizer").getUsage());
            }
        }
    }

    @Override
    public void onEnable() {
        getColoredLogger().sendMessage(PREFIX + "Loading ConsoleOptimizer 1.1...");
        getColoredLogger().sendMessage(PREFIX + "Author: Ringwid");
        saveDefaultConfig();
        try {
            BufferedWriter writer = getBufferedWriter();
            Field field1 = writer.getClass().getDeclaredField("out");
            field1.setAccessible(true);
            this.outStreamBackup = (Writer) field1.get(writer);
            field1.set(writer, new OutputFilter(this, outStreamBackup));
        } catch (Exception e) {
            getColoredLogger().sendMessage(ChatColor.RED + "Unable to load ConsoleOptimizer.");
        }
        this.config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);

        if (getConfig().getBoolean("interceptErrors")) {
            getColoredLogger().sendMessage(PREFIX + "Errors are now intercepted.");
            interceptErrors(true);
        }
        getColoredLogger().sendMessage(PREFIX + ChatColor.GREEN + "ConsoleOptimizer loaded.");
    }

    private void interceptErrors(boolean b) {
        try {
            Field field = System.err.getClass().getDeclaredField("textOut");
            field.setAccessible(true);
            BufferedWriter bufferedWriter = (BufferedWriter) field.get(System.err);
            Field field1 = bufferedWriter.getClass().getDeclaredField("out");
            field1.setAccessible(true);

            if (b && !(errStreamBackup == null)) {
                return;
            }

            if (b) {
                errStreamBackup = (Writer) field1.get(bufferedWriter);
                field1.set(bufferedWriter, new ErrorOutputFilter(this, errStreamBackup));
            } else {
                field1.set(bufferedWriter, errStreamBackup);
            }
        } catch (Exception e) {
            e.printStackTrace();
            getColoredLogger().sendMessage(ChatColor.RED + "Unable to load ConsoleOptimizer.");
        }
    }

    @Override
    public void onDisable() {
        getColoredLogger().sendMessage(PREFIX + "Disabling ConsoleOptimizer...");
        if (getConfig().getBoolean("interceptErrors") || errStreamBackup != null) {
            interceptErrors(false);
        }
        try {
            BufferedWriter writer = getBufferedWriter();
            Field field1 = writer.getClass().getDeclaredField("out");
            field1.setAccessible(true);
            field1.set(writer, outStreamBackup);
        } catch (Exception e) {
            getColoredLogger().sendMessage(ChatColor.RED + "Unable to disable ConsoleOptimizer.");
        }
        getColoredLogger().sendMessage(PREFIX + "ConsoleOptimizer disabled...");
    }

    private BufferedWriter getBufferedWriter() throws Exception {
        Field field = System.out.getClass().getDeclaredField("textOut");
        field.setAccessible(true);
        return (BufferedWriter) field.get(System.out);
    }

    public ConsoleCommandSender getColoredLogger() {
        return getServer().getConsoleSender();
    }

    public List<String> getErrorLog() {
        return errorLog;
    }

    public HashMap<String, List<String>> getPluginLog() {
        return pluginLog;
    }

}
