package com.shaoxinpipaqu.qa.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric 1.21.1 验收窗口在创建时隐藏，保留真实 OpenGL 渲染。 */
@Mixin(Window.class)
public abstract class BackgroundWindowMixin {
    @Inject(method="<init>",at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",remap=false))
    private void probe$background(CallbackInfo ci) {
        if(Boolean.getBoolean("pipa.qa.foreground"))return;
        // 在原版设置窗口提示之后覆盖，避免窗口先显示再隐藏时短暂抢走焦点。
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW,GLFW.GLFW_FALSE);
        // 本机隐藏验收沿用已验证的 4.6 上下文，避免退回原版 3.2 渲染路径。
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,6);
    }
}
