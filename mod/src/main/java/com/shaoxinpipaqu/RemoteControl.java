package com.shaoxinpipaqu;

import com.friendswine.DollBlockEntity;
import com.friendswine.FriendsWine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;

/** 服务端重新核验持物、维度及区块，不接受客户端传入的玩偶坐标。 */
public final class RemoteControl {
    private RemoteControl() {}
    public static boolean held(Player p) { return p.getMainHandItem().is(FriendsWine.REMOTE.get()); }
    /** 网络入口已切回服务端线程；防止重复包每刻切歌并重抽飞行名单。 */
    public static void receive(Player player) {
        long now=player.level().getGameTime();
        var data=Pipa.data(player);
        if (data.contains("PipaLastSwitch") && now>=data.getLong("PipaLastSwitch") && now-data.getLong("PipaLastSwitch")<4) return;
        data.putLong("PipaLastSwitch",now);
        switchSong(player);
    }
    public static void switchSong(Player p) {
        if (!held(p) || p.isSpectator() || p.level().isClientSide) return;
        CompoundTag t = p.getMainHandItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("FriendsWineTarget");
        String error;
        if (!t.contains("Dimension", Tag.TAG_STRING) || !t.contains("Pos", Tag.TAG_LONG)) error = "remote_unbound";
        else if (!t.getString("Dimension").equals(p.level().dimension().location().toString())) error = "remote_dimension";
        else {
            BlockPos pos = BlockPos.of(t.getLong("Pos"));
            if (p.level().hasChunkAt(pos) && p.level().getBlockEntity(pos) instanceof DollBlockEntity doll) {
                SongState.of(doll).pipa$toggle();
                p.displayClientMessage(Component.translatable("message.shaoxinpipaqu.song", Component.translatable(SongState.of(doll).pipa$isSelected() ? "song.shaoxinpipaqu.pipa" : "song.shaoxinpipaqu.wine")), true);
                return;
            }
            error = "remote_unavailable";
        }
        p.displayClientMessage(Component.translatable("message.friendswine." + error), true);
    }
}
