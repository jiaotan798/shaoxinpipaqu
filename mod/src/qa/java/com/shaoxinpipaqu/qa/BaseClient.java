package com.shaoxinpipaqu.qa;

import com.shaoxinpipaqu.*;
import java.nio.file.Files;
import net.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** 与无可选依赖专服配套，只用原版类型驱动按键并检查真实同步和后台窗口约束。 */
@Mod(value="pipa_qa",dist=Dist.CLIENT)
public final class BaseClient {
    private final String role=System.getProperty("pipa.qa.role");
    private int stage=-1,age,flightStart;private boolean done;
    public BaseClient(){if(Boolean.getBoolean("pipa.qa.baseOnly")&&!Boolean.getBoolean("pipa.qa.memeOnly")&&!Boolean.getBoolean("pipa.qa.promo"))NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event)->tick());}
    private void tick() {
        if(done)return;var mc=Minecraft.getInstance();
        try {
            if(mc.getWindow()==null)return;
            if(!Boolean.getBoolean("pipa.qa.foreground")) {
                long window=mc.getWindow().getWindow();
                BaseCheck.require(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_VISIBLE)==GLFW.GLFW_FALSE&&GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)==GLFW.GLFW_FALSE&&!mc.mouseHandler.isMouseGrabbed(),"无可选依赖专项也必须隐藏、无焦点、不捕鼠标");
            }
            if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen&&Files.exists(BaseCheck.ROOT.resolve(role+"-connected.ok")))throw new AssertionError("基础依赖专项意外断线，见原始日志");
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen!=null)return;
            BaseCheck.require(mc.getSingleplayerServer()==null,"必须使用真实TCP专服");
            if(!Files.exists(BaseCheck.ROOT.resolve(role+"-connected.ok")))BaseCheck.write(role+"-connected.ok","PASS TCP connected\n");
            int next=Integer.parseInt(Files.readString(BaseCheck.ROOT.resolve("stage.txt")).trim());BaseCheck.require(next!=-1,"专服断言失败");
            if(next==99){mc.options.keyJump.setDown(false);BaseCheck.write(role+"-window.txt","PASS hidden/unfocused/no mouse capture checked each tick\n");BaseCheck.write(role+"-result.txt","PASS no optional models; native player and effects synchronized\n");done=true;mc.stop();return;}
            if(next!=stage){stage=next;age=0;mc.options.keyJump.setDown(false);}age++;
            if(stage==0)return;
            if(stage==1&&role.equals("actor")) {
                // 先等权限真正同步再双击，避免世界初次加载时的网络排队吞掉第一次起飞按键。
                if(flightStart==0&&mc.player.hasEffect(Pipa.MILK)&&mc.player.getAbilities().mayfly)flightStart=age;
                int inputAge=age-flightStart;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.keyJump.setDown(flightStart>0&&(inputAge==10||inputAge>=15&&inputAge<45));
            }
            if(age<90||Files.exists(BaseCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
            if(stage==1&&(!mc.player.hasEffect(Pipa.MILK)||!mc.player.hasEffect(Pipa.PEACE)))return;
            if(stage==2&&(mc.player.hasEffect(Pipa.MILK)||MobFlight.posed(mc.player)))return;
            if(stage==1&&role.equals("actor")&&!MobFlight.posed(mc.player))return;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(BaseCheck.ROOT.resolve(role+"-stage-"+stage+".png"));}
            BaseCheck.write(role+"-"+stage+".ok","PASS actual effect synchronization and GPU frame\n");
        }catch(Throwable error){error.printStackTrace();mc.options.keyJump.setDown(false);BaseCheck.write(role+"-result.txt","FAIL: "+error);done=true;mc.stop();}
    }
}
