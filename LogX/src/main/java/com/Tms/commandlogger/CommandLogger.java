import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommandLogger extends JavaPlugin implements Listener {

    // 通用配置
    private static final int MAX_LOG_SIZE = 100 * 1024; // 100KB
    private static final SimpleDateFormat FILE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    // 普通命令日志（玩家+控制台）
    private BufferedWriter normalLogWriter;
    private File normalLogFile;
    private long normalLogSize;
    
    // 命令方块专用日志
    private BufferedWriter commandBlockLogWriter; 
    private File commandBlockLogFile;
    private long commandBlockLogSize;

    @Override
    public void onEnable() {
        try {
            // 初始化日志系统
            initNormalLogger();
            initCommandBlockLogger();
            
            getServer().getPluginManager().registerEvents(this, this);
        } catch (IOException e) {
            getLogger().severe("日志系统初始化失败: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // 初始化普通命令日志
    private void initNormalLogger() throws IOException {
        File logDir = new File(getDataFolder(), "normal-logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            throw new IOException("无法创建普通日志目录");
        }
        normalLogFile = new File(logDir, FILE_FORMAT.format(new Date()) + ".log");
        normalLogWriter = new BufferedWriter(new FileWriter(normalLogFile, true));
        normalLogSize = normalLogFile.length();
    }

    // 初始化命令方块日志
    private void initCommandBlockLogger() throws IOException {
        File logDir = new File(getDataFolder(), "commandblock-logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            throw new IOException("无法创建命令方块日志目录");
        }
        commandBlockLogFile = new File(logDir, FILE_FORMAT.format(new Date()) + ".log");
        commandBlockLogWriter = new BufferedWriter(new FileWriter(commandBlockLogFile, true));
        commandBlockLogSize = commandBlockLogFile.length();
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        logNormalCommand(
            event.getPlayer().getName(),
            event.getMessage(),
            "PLAYER"
        );
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        CommandSender sender = event.getSender();
        
        if (sender instanceof BlockCommandSender) {
            BlockCommandSender blockSender = (BlockCommandSender) sender;
            logCommandBlockCommand(
                blockSender.getBlock(),
                event.getCommand()
            );
        } else if (sender instanceof ConsoleCommandSender) {
            logNormalCommand(
                "CONSOLE",
                event.getCommand(),
                "SERVER"
            );
        }
    }

    // 记录普通命令（玩家/控制台）
    private synchronized void logNormalCommand(String sender, String command, String type) {
        try {
            String entry = String.format("[%s] %s: %s%n",
                new SimpleDateFormat("HH:mm:ss").format(new Date()),
                sender, 
                command);
            
            writeWithRotation(
                entry, 
                normalLogWriter, 
                normalLogFile, 
                normalLogSize,
                this::initNormalLogger
            );
            
        } catch (IOException e) {
            getLogger().severe("普通日志写入失败: " + e.getMessage());
        }
    }

    // 记录命令方块命令
    private synchronized void logCommandBlockCommand(Block block, String command) {
        try {
            String location = String.format("%s (%d,%d,%d)",
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ());
            
            String entry = String.format("[%s] %s: %s%n",
                new SimpleDateFormat("HH:mm:ss").format(new Date()),
                location,
                command);
            
            writeWithRotation(
                entry, 
                commandBlockLogWriter, 
                commandBlockLogFile, 
                commandBlockLogSize,
                this::initCommandBlockLogger
            );
            
        } catch (IOException e) {
            getLogger().severe("命令方块日志写入失败: " + e.getMessage());
        }
    }

    // 通用写入方法（带自动轮转）
    private void writeWithRotation(String entry, 
                                  BufferedWriter writer,
                                  File currentFile,
                                  long currentSize,
                                  Runnable rotator) throws IOException {
        byte[] bytes = entry.getBytes();
        
        // 检查文件大小
        if (currentSize + bytes.length > MAX_LOG_SIZE) {
            rotator.run(); // 执行轮转
            currentSize = 0; // 重置大小计数器
        }
        
        writer.write(entry);
        writer.flush();
        
        // 更新大小计数器
        if (writer == normalLogWriter) {
            normalLogSize += bytes.length;
        } else {
            commandBlockLogSize += bytes.length;
        }
    }

    @Override
    public void onDisable() {
        closeWriter(normalLogWriter, "普通日志");
        closeWriter(commandBlockLogWriter, "命令方块日志");
    }

    private void closeWriter(BufferedWriter writer, String logType) {
        try {
            if (writer != null) {
                writer.close();
                getLogger().info(logType + "写入器已关闭");
            }
        } catch (IOException e) {
            getLogger().warning("关闭" + logType + "写入器失败: " + e.getMessage());
        }
    }
}