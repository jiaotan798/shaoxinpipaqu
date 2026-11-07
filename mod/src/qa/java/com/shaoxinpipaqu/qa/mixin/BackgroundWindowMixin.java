package com.shaoxinpipaqu.qa.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 仅验收探针：创建隐藏但正常渲染的窗口，避免先显示再隐藏时短暂抢走焦点。 */
@Mixin(Window.class)
public abstract class BackgroundWindowMixin {
    @Inject(method="<init>",at=@At(value="INVOKE",target="Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J",remap=false))
    private void probe$background(CallbackInfo ci) {
        if(Boolean.getBoolean("pipa.qa.foreground"))return;
        // 配套启动器禁用早期窗口；这里必须晚于默认 hints、早于实际创建 OpenGL 窗口。
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW,GLFW.GLFW_FALSE);
        // 本机旧验收由早期窗口提供4.6；禁用它后须保留相同上下文，避免光影退到原版3.2路径。
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,6);
    }
}
