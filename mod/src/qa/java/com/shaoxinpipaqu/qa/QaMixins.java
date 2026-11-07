package com.shaoxinpipaqu.qa;

import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import net.neoforged.fml.loading.LoadingModList;

/** 关闭YSM的原版玩家专项仍需其余探针；不能让可选YSM捕获类阻止客户端启动。 */
public final class QaMixins implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target,String mixin) {
        return !mixin.contains("Ysm") || LoadingModList.get().getMods().stream().anyMatch(m->m.getModId().equals("yes_steve_model"));
    }
    @Override public void onLoad(String p) {}
    @Override public String getRefMapperConfig() {return null;}
    @Override public void acceptTargets(Set<String> mine,Set<String> other) {}
    @Override public List<String> getMixins() {return null;}
    @Override public void preApply(String name,ClassNode node,String mixin,IMixinInfo info) {}
    @Override public void postApply(String name,ClassNode node,String mixin,IMixinInfo info) {}
}
