package com.shaoxinpipaqu.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.shaoxinpipaqu.Pipa;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** 卡点开关只读本机配置；Fabric 客户端命令不要求服务器权限。 */
public final class ClientConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(Pipa.ID + "-client.properties");
    private static final Properties VALUES = new Properties();
    public static final Toggle SHOW_CUE_TEXT = new Toggle("showCueText");
    public static final Toggle SHOW_MEME_PICTURES = new Toggle("showMemePictures");

    private ClientConfig() {}

    static void load() {
        if (Files.isRegularFile(FILE)) {
            try (var input = Files.newBufferedReader(FILE)) { VALUES.load(input); }
            catch (IOException | IllegalArgumentException error) {
                VALUES.clear();
                LoggerFactory.getLogger(Pipa.ID).warn("无法读取本机卡点设置", error);
            }
        }
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            var settings = literal("settings");
            for (Toggle toggle : new Toggle[]{SHOW_CUE_TEXT, SHOW_MEME_PICTURES}) {
                settings.then(literal(toggle.key()).then(argument("enabled", BoolArgumentType.bool()).executes(command -> {
                    boolean enabled = BoolArgumentType.getBool(command, "enabled");
                    try { toggle.save(enabled); }
                    catch (IOException error) {
                        command.getSource().sendError(Component.literal("无法保存本机卡点设置"));
                        return 0;
                    }
                    command.getSource().sendFeedback(Component.translatable(Pipa.ID + ".config." + toggle.key())
                            .append(" ").append(Component.translatable(enabled ? "options.on" : "options.off")));
                    return 1;
                })));
            }
            dispatcher.register(literal(Pipa.ID).then(settings));
        });
    }

    public record Toggle(String key) {
        public boolean get() { return !"false".equalsIgnoreCase(VALUES.getProperty(key)); }

        private void save(boolean enabled) throws IOException {
            var next = new Properties();
            next.putAll(VALUES);
            next.setProperty(key, Boolean.toString(enabled));
            // 文件写完整后才替换；磁盘写入失败时保留原配置和运行值。
            var pending = Files.createTempFile(FILE.getParent(), Pipa.ID, ".tmp");
            try {
                try (var output = Files.newBufferedWriter(pending)) { next.store(output, null); }
                try { Files.move(pending, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException unavailable) { Files.move(pending, FILE, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(pending); }
            VALUES.putAll(next);
        }
    }
}
