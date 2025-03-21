package com.Tms;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.block.BlockCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class CommandLoggerPlugin extends JavaPlugin implements Listener {

    private int commandBlockCount = 0;
    private File commandBlockLogDirectory;
    private File currentCommandBlockLogFile;
    private FileWriter commandBlockLogWriter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.commandBlockLogDirectory = new File(getDataFolder(), "cmds/logs/command_block");

        if (!commandBlockLogDirectory.exists()) {
            commandBlockLogDirectory.mkdirs();
        }

        getServer().getPluginManager().registerEvents(this, this);
        createNewLogFile();

        getLogger().info("CommandLogger Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        try {
            if (commandBlockLogWriter != null) {
                commandBlockLogWriter.close();
            }
        } catch (IOException e) {
            getLogger().warning("Failed to close log files: " + e.getMessage());
        }
        getLogger().info("CommandLogger Plugin Disabled!");
    }

    private void createNewLogFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());

        // For command block commands
        int commandBlockLogNumber = getLogFileCount(commandBlockLogDirectory) + 1;
        currentCommandBlockLogFile = new File(commandBlockLogDirectory, timestamp + "-" + commandBlockLogNumber + ".log");

        try {
            if (!currentCommandBlockLogFile.exists()) {
                currentCommandBlockLogFile.createNewFile();
            }
            commandBlockLogWriter = new FileWriter(currentCommandBlockLogFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getLogFileCount(File logDirectory) {
        File[] files = logDirectory.listFiles((dir, name) -> name.endsWith(".log"));
        return files == null ? 0 : files.length;
    }

    @EventHandler
    public void onBlockCommand(BlockCommandEvent event) {
        BlockCommandSender sender = event.getBlockCommandSender();
        String command = event.getCommand();
        String coordinates = sender.getBlock().getX() + "," + sender.getBlock().getY() + "," + sender.getBlock().getZ() + "," + sender.getBlock().getWorld().getName();

        // Execute the command and check result
        boolean success = executeCommand(sender, command);
        logCommandBlock(sender.getName(), coordinates, command, success);
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        CommandSender sender = event.getSender();
        String command = event.getCommand();

        // Execute the command and check result
        boolean success = executeCommand(sender, command);
        logCommand(sender.getName(), command, success);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage();

        // Execute the command and check result
        boolean success = executeCommand(player, command);
        logCommand(player.getName(), command, success);
    }

    private boolean executeCommand(CommandSender sender, String command) {
        try {
            // Dispatch the command and check if it's successful (this is where you would ideally check success)
            Command cmd = getServer().getCommandMap().getCommand(command.split(" ")[0]);

            if (cmd != null) {
                return cmd.execute(sender, command.split(" ")[0], command.split(" "));
            }
        } catch (Exception e) {
            getLogger().warning("Error executing command: " + command + " - " + e.getMessage());
            return false;
        }

        return false;
    }

    private void logCommand(String senderName, String command, boolean success) {
        commandBlockCount++;

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        String logLine = String.format("%d [%s] [%s] [%s] [%s]\n", commandBlockCount, timestamp, senderName, command, success ? "Success" : "Failure");

        try {
            commandBlockLogWriter.write(logLine);
        } catch (IOException e) {
            getLogger().warning("Failed to log command: " + e.getMessage());
        }
    }

    private void logCommandBlock(String senderName, String coordinates, String command, boolean success) {
        commandBlockCount++;

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        String logLine = String.format("%d [%s] [CommandBlock at %s] [%s] [%s]\n",
                commandBlockCount, timestamp, coordinates, command, success ? "Success" : "Failure");

        try {
            commandBlockLogWriter.write(logLine);
        } catch (IOException e) {
            getLogger().warning("Failed to log command block command: " + e.getMessage());
        }
    }
}
