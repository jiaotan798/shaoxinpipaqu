package com.shaoxinpipaqu.compat;

import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import net.neoforged.fml.loading.LoadingModList;

/** 可选桥接严格绑定已核对的二进制版本，缺模组时不加载包含其类型的 Mixin。 */
public final class CompatMixins implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target,String mixin) {
        String id=mixin.contains("YsmFlight")?"yes_steve_model":mixin.contains("Maid")?"touhou_little_maid":null;
        if(id==null) return true;
        String version=id.equals("yes_steve_model")?"2.6.5-neoforge+mc1.21.1":"1.5.3-neoforge+mc1.21.1";
        return LoadingModList.get().getMods().stream().anyMatch(m -> m.getModId().equals(id) && m.getVersion().toString().equals(version));
    }
    @Override public void onLoad(String p) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine,Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name,ClassNode node,String mixin,IMixinInfo info) {}
    @Override public void postApply(String name,ClassNode node,String mixin,IMixinInfo info) {}
}
