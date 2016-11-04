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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

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
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "help":
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "------------ " + ChatColor.YELLOW + "ConsoleOptimizer Command List: " + ChatColor.LIGHT_PURPLE + "------------");
                sender.sendMessage(ChatColor.AQUA + "- help:" + ChatColor.GRAY + ChatColor.ITALIC + " Show the help page");
                sender.sendMessage(ChatColor.AQUA + "- filter add plugin <plugin>:" + ChatColor.GRAY + ChatColor.ITALIC + " Add a filter of the specified plugin");
                sender.sendMessage(ChatColor.AQUA + "- filter remove plugin <plugin>:" + ChatColor.GRAY + ChatColor.ITALIC + " Remove the filter of the specified plugin");
                sender.sendMessage(ChatColor.AQUA + "- filter add keyword <keyword>:" + ChatColor.GRAY + ChatColor.ITALIC + " Filter all messages contains the keyword.");
                sender.sendMessage(ChatColor.AQUA + "- filter remove keyword <keyword>:" + ChatColor.GRAY + ChatColor.ITALIC + " Remove the filter of the specified keyword.");
                sender.sendMessage(ChatColor.AQUA + "- filter add errors:" + ChatColor.GRAY + ChatColor.ITALIC + " Add the error filter");
                sender.sendMessage(ChatColor.AQUA + "- filter remove errors:" + ChatColor.GRAY + ChatColor.ITALIC + " Remove the error filter");
                sender.sendMessage(ChatColor.AQUA + "- showlog <plugin>:" + ChatColor.GRAY + ChatColor.ITALIC + " Show all the messages intercepted by ConsoleOptimizer");
                sender.sendMessage(ChatColor.AQUA + "- showerrors:" + ChatColor.GRAY + ChatColor.ITALIC + " Show all the errors intercepted by ConsoleOptimizer");
                break;
            case "filter":
                if (args.length < 3) {
                    sender.sendMessage("Usage: /co filter <Add/Remove> <Keyword/Plugin Name/Errors> [Name]");
                    break;
                }

                String action = args[1].toLowerCase();
                String actionType = args[2].toLowerCase();

                if (!(action.equals("add") || action.equals("remove")) || (!actionType.equals("error") && args.length < 4)) {
                    sender.sendMessage("Usage: /co filter <Add/Remove> <Keyword/Plugin Name/Errors> [Name]");
                }
                
                switch (actionType) {
                    case "errors":
                        config.set("interceptErrors", action.equals("add"));
                        if (action.equals("add")) {
                            interceptErrors(true);
                        } else {
                            interceptErrors(false);
                        }
                        sender.sendMessage(PREFIX + "Succeed.");
                        break;
                    case "plugin":
                        String pluginName = args[3];
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
                                sender.sendMessage(PREFIX + "The plugin's output is already intercepted.");
                            } else {
                                list.add(pluginName);
                                sender.sendMessage(PREFIX + "Succeed.");
                            }
                        } else {
                            if (!list.contains(pluginName)) {
                                sender.sendMessage(PREFIX + "The plugin's output isn't intercepted.");
                            } else {
                                list.remove(pluginName);
                                sender.sendMessage(PREFIX + "Succeed.");
                            }
                        }
                        config.set("interceptPluginList", list);
                        break;
                    case "keyword":
                        String keyword = args[3];
                        match = false;
                        for (Plugin plugin : getServer().getPluginManager().getPlugins()) {
                            if (plugin.getName().equals(keyword)) {
                                match = true;
                            }
                        }
                        if (!match) {
                            sender.sendMessage(PREFIX + "The plugin you have entered doesn't exists!");
                            break;
                        }

                        list = config.getStringList("interceptKeywordList");
                        if (action.equals("add")) {
                            if (list.contains(keyword)) {
                                sender.sendMessage(PREFIX + "Messages contains the keyword is already intercepted.");
                            } else {
                                list.add(keyword);
                                sender.sendMessage(PREFIX + "Succeed.");
                            }
                        } else {
                            if (!list.contains(keyword)) {
                                sender.sendMessage(PREFIX + "Messages contains the keyword isn't intercepted.");
                            } else {
                                list.remove(keyword);
                                sender.sendMessage(PREFIX + "Succeed.");
                            }
                        }
                        config.set("interceptKeywordList", list);
                        break;
                    default:
                        sender.sendMessage("Usage: /co filter <Add/Remove> <Keyword/Plugin Name/Errors> [Name]");
                }
                saveConfig();
//                if (actionType.equals("ConsoleOptimizer")) {
//                    sender.sendMessage(PREFIX + "Nope!");
//                    break;
//                } else {
//                    if (!actionType.equals("errors")) {
//                        boolean match = false;
//                        for (Plugin plugin : getServer().getPluginManager().getPlugins()) {
//                            if (plugin.getName().equals(actionType)) {
//                                match = true;
//                            }
//                        }
//                        if (!match) {
//                            sender.sendMessage(PREFIX + "The plugin you have entered doesn't exists!");
//                            break;
//                        }
//
//                        List<String> list = config.getStringList("interceptPluginList");
//                        if (action.equals("add")) {
//                            if (list.contains(actionType)) {
//                                sender.sendMessage(PREFIX + "The plugin's output is already intercepted.");
//                            } else {
//                                list.add(actionType);
//                                sender.sendMessage(PREFIX + "Succeed.");
//                            }
//                        } else {
//                            if (!list.contains(actionType)) {
//                                sender.sendMessage(PREFIX + "The plugin's output isn't intercepted.");
//                            } else {
//                                list.remove(actionType);
//                                sender.sendMessage(PREFIX + "Succeed.");
//                            }
//                        }
//                        config.set("interceptPluginList", list);
//                    } else {
//                        config.set("interceptErrors", action.equals("add"));
//                        if (action.equals("add")) {
//                            interceptErrors(true);
//                        } else {
//                            interceptErrors(false);
//                        }
//                        sender.sendMessage(PREFIX + "Succeed.");
//                    }
//
//                    saveConfig();
//                }
                break;
            case "showlog":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /co showlog <pluginName>");
                    break;
                }
                actionType = args[1];
                if (pluginLog.get(actionType) == null) {
                    sender.sendMessage(PREFIX + "The plugin " + actionType + " has no log.");
                    break;
                }
                pluginLog.get(actionType).forEach(sender::sendMessage);
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
            event.setCommand(event.getCommand().replaceFirst("co", "consoleoptimizer"));
        }
    }

    @Override
    public void onEnable() {
        getColoredLogger().sendMessage(PREFIX + "Loading ConsoleOptimizer 1.1.2...");
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

            if (b) {
                if (errStreamBackup != null) {
                    return;
                }
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
        getColoredLogger().sendMessage(PREFIX + "Disabling ConsoleOptimizer 1.1.2...");
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
