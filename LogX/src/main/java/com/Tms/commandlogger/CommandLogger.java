import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class CommandLogger extends JavaPlugin implements Listener {

    // 配置缓存
    private final Set<String> ignoredCoordinates = new HashSet<>();
    private int mergeWindow;
    private int maxMergeCount;

    // 日志系统
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private BufferedWriter logWriter;
    private File currentLogFile;

    // 命令合并缓存
    private final Map<String, CommandRecord> commandCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // 命令记录类
    private static class CommandRecord {
        final long firstExecute;
        long lastExecute;
        int count;
        final String command;

        CommandRecord(String command) {
            this.firstExecute = System.currentTimeMillis();
            this.lastExecute = this.firstExecute;
            this.count = 1;
            this.command = command;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadConfig();

        try {
            initLogger();
            startTasks();
        } catch (IOException e) {
            getLogger().severe("日志系统初始化失败: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        
        // 加载忽略坐标（格式：world,x,y,z）
        ignoredCoordinates.clear();
        for (String coord : config.getStringList("ignore-commandblocks")) {
            ignoredCoordinates.add(coord.replace(" ", "").toLowerCase());
        }

        // 加载合并配置
        mergeWindow = config.getInt("command-merge.window", 3000);
        maxMergeCount = config.getInt("command-merge.max-count", 10);
    }

    private void initLogger() throws IOException {
        File logDir = new File(getDataFolder(), "commandblock-logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            throw new IOException("无法创建日志目录");
        }
        currentLogFile = new File(logDir, dateFormat.format(new Date()) + ".log");
        logWriter = new BufferedWriter(new FileWriter(currentLogFile, true));
    }

    private void startTasks() {
        // 每5秒清理一次缓存
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            commandCache.entrySet().removeIf(entry -> 
                now - entry.getValue().lastExecute > mergeWindow
            );
        }, 5, 5, TimeUnit.SECONDS);

        // 每天轮转日志
        scheduler.scheduleAtFixedRate(() -> {
            try {
                rotateLogFile();
            } catch (IOException e) {
                getLogger().warning("日志轮转失败: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.DAYS);
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (!(event.getSender() instanceof BlockCommandSender)) return;

        BlockCommandSender sender = (BlockCommandSender) event.getSender();
        Block block = sender.getBlock();
        String world = block.getWorld().getName();
        
        // 检查忽略坐标（格式：world,x,y,z）
        String coordKey = String.format("%s,%d,%d,%d", 
            world.toLowerCase(),
            block.getX(),
            block.getY(),
            block.getZ());
        if (ignoredCoordinates.contains(coordKey)) return;

        processCommand(block, event.getCommand());
    }

    private void processCommand(Block block, String command) {
        String world = block.getWorld().getName();
        String cacheKey = String.format("%s|%d|%d|%d|%d", 
            world,
            block.getX(),
            block.getY(),
            block.getZ(),
            command.hashCode());

        long currentTime = System.currentTimeMillis();
        
        commandCache.compute(cacheKey, (key, record) -> {
            if (record == null) {
                return new CommandRecord(command);
            }

            // 相同命令且在时间窗口内
            if (record.command.equals(command) && 
                currentTime - record.firstExecute <= mergeWindow) {
                record.count++;
                record.lastExecute = currentTime;
                return record;
            }

            // 不同命令或超出时间窗口，立即写入
            writeLogEntry(block, record);
            return new CommandRecord(command);
        });
    }

    private synchronized void writeLogEntry(Block block, CommandRecord record) {
        try {
            String logEntry = buildLogEntry(block, record);
            logWriter.write(logEntry);
            logWriter.flush();
        } catch (IOException e) {
            getLogger().warning("日志写入失败: " + e.getMessage());
        }
    }

    private String buildLogEntry(Block block, CommandRecord record) {
        String base = String.format("[%s] %s (%d,%d,%d) - ",
            timeFormat.format(new Date(record.firstExecute)),
            block.getWorld().getName(),
            block.getX(),
            block.getY(),
            block.getZ());

        if (record.count > 1) {
            return base + String.format("执行 %s 共 %d 次（最后执行于 %s）%n",
                record.command,
                record.count,
                timeFormat.format(new Date(record.lastExecute)));
        }
        return base + record.command + "\n";
    }

    private void rotateLogFile() throws IOException {
        logWriter.close();
        initLogger();
        cleanupOldLogs();
    }

    private void cleanupOldLogs() {
        File logDir = new File(getDataFolder(), "commandblock-logs");
        long cutoff = System.currentTimeMillis() - 
            TimeUnit.DAYS.toMillis(getConfig().getInt("log-rotate.keep-days", 7));

        for (File file : logDir.listFiles()) {
            if (file.lastModified() < cutoff) {
                file.delete();
            }
        }
    }

    @Override
    public void onDisable() {
        try {
            // 写入剩余缓存
            commandCache.forEach((k, v) -> writeLogEntry(
                ((BlockCommandSender) Bukkit.getServer().getConsoleSender()).getBlock(), v));
            logWriter.close();
            scheduler.shutdownNow();
        } catch (IOException e) {
            getLogger().warning("关闭日志失败: " + e.getMessage());
        }
    }
}
