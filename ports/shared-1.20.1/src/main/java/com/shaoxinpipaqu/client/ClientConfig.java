package com.shaoxinpipaqu.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 两种加载器共用本机显示偏好，服务端不读取客户端配置。 */
public final class ClientConfig {
    static boolean showCueText = true, showMemePictures = true;

    private ClientConfig() {}

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/shaoxinpipaqu-client.properties");
    }

    public static void load() {
        if (!Files.isRegularFile(path())) return;
        try (var reader = Files.newBufferedReader(path())) {
            Properties config = new Properties();
            config.load(reader);
            showCueText = !"false".equalsIgnoreCase(config.getProperty("showCueText"));
            showMemePictures = !"false".equalsIgnoreCase(config.getProperty("showMemePictures"));
        } catch (IOException | IllegalArgumentException failure) {
            LogUtils.getLogger().warn("无法读取烧心琵琶曲显示设置", failure);
        }
    }

    public static Screen screen(Screen parent) {
        return new Screen(Component.translatable("shaoxinpipaqu.configuration.title")) {
            @Override protected void init() {
                addRenderableWidget(Button.builder(label("showCueText", showCueText), button -> {
                    if (save(!showCueText, showMemePictures)) {
                        showCueText = !showCueText;
                        button.setMessage(label("showCueText", showCueText));
                    }
                }).bounds(width / 2 - 100, height / 2 - 32, 200, 20).build());
                addRenderableWidget(Button.builder(label("showMemePictures", showMemePictures), button -> {
                    if (save(showCueText, !showMemePictures)) {
                        showMemePictures = !showMemePictures;
                        button.setMessage(label("showMemePictures", showMemePictures));
                    }
                }).bounds(width / 2 - 100, height / 2 - 8, 200, 20).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                        .bounds(width / 2 - 100, height / 2 + 24, 200, 20).build());
            }

            @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                renderBackground(graphics);
                graphics.drawCenteredString(font, title, width / 2, height / 2 - 56, 0xffffff);
                super.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override public void onClose() { minecraft.setScreen(parent); }
        };
    }

    private static Component label(String key, boolean enabled) {
        return Component.translatable("shaoxinpipaqu.config." + key)
                .append(": ").append(Component.translatable(enabled ? "options.on" : "options.off"));
    }

    /** 仅在完整配置已原子替换时提交显示状态，写入失败时旧值和按钮文案继续一致。 */
    private static boolean save(boolean cueText, boolean memePictures) {
        Properties config = new Properties();
        config.setProperty("showCueText", Boolean.toString(cueText));
        config.setProperty("showMemePictures", Boolean.toString(memePictures));
        Path target = path();
        Path pending = null;
        try {
            Files.createDirectories(target.getParent());
            // 同目录保证临时文件和目标位于同一文件系统；不支持原子替换时保留原文件。
            pending = Files.createTempFile(target.getParent(), "shaoxinpipaqu-", ".tmp");
            try (var writer = Files.newBufferedWriter(pending)) { config.store(writer, null); }
            Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            pending = null;
            return true;
        } catch (IOException | SecurityException failure) {
            LogUtils.getLogger().warn("无法保存烧心琵琶曲显示设置", failure);
            return false;
        } finally {
            if (pending != null) {
                try { Files.deleteIfExists(pending); }
                catch (IOException | SecurityException failure) {
                    LogUtils.getLogger().warn("无法清理烧心琵琶曲设置临时文件", failure);
                }
            }
        }
    }
}
