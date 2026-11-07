package com.shaoxinpipaqu.compat;

import com.elfmcys.yesstevemodel.*;
import com.shaoxinpipaqu.client.FlightVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
import org.slf4j.LoggerFactory;

/** YSM 2.6.5 动画完成后的骨骼覆盖；持续到手持物等全部图层结束，主通道和阴影通道分别恢复。 */
public final class YsmFlightPose implements AutoCloseable {
    private final float[] transforms;
    private final float[] saved;
    private static final Set<String> WARNED=new HashSet<>();
    private YsmFlightPose(float[] transforms) { this.transforms=transforms; saved=transforms.clone(); }

    /** 数组每骨 12 float：0～2 旋转、3～5 偏移、6～8 缩放；由发布 JAR 的骨骼访问器核实，单位为弧度。 */
    public static YsmFlightPose apply(o0ooO0ooO00oo0o00Oo00000 skeleton,OoO0oo0o0o0oOoo0oOOO0Ooo<?> actor,PoseStack pose) {
        if(!(actor.ooo00OoO00OOOO0oOooOo0Oo() instanceof LivingEntity entity) || FlightVisuals.weight(entity)<=0) return null;
        var bones=skeleton.OOo0o0000Ooo0o00OO0oOOoO().oOo0OO0O0o000OO0O000oo0o();
        if(bones.stream().noneMatch(b -> FlightBones.role(b.oOo0OO0O0o000OO0O000oo0o())==2)) {
            if(WARNED.add(actor.b_().toString())) LoggerFactory.getLogger("shaoxinpipaqu").warn("模型 {} 没有可识别的右上臂，保留模型姿态，需单独适配",actor.b_());
            return null;
        }
        float[] data=skeleton.oOo0OO0O0o000OO0O000oo0o();
        if(data.length!=bones.size()*12) throw new IllegalStateException("YSM 骨骼缓冲布局不匹配");
        var scope=new YsmFlightPose(data);
        float weight=FlightVisuals.weight(entity);
        for(int i=0;i<bones.size();i++) {
            int role=FlightBones.role(bones.get(i).oOo0OO0O0o000OO0O000oo0o());
            if(role==0) continue;
            int offset=i*12;
            data[offset]=FlightVisuals.blend(data[offset],FlightBones.pitch(role,true),weight);
            data[offset+1]*=1-weight;
            data[offset+2]=FlightVisuals.blend(data[offset+2],FlightBones.roll(role),weight);
            for(int component=3;component<6;component++)data[offset+component]*=1-weight;
        }
        // 调用点已位于 YSM 自身的 push/pop 内；保持变换覆盖身体和手持物，不能只包围最终身体绘制。
        FlightVisuals.body(pose,entity,true);
        return scope;
    }
    @Override public void close() { System.arraycopy(saved,0,transforms,0,saved.length); }
}
