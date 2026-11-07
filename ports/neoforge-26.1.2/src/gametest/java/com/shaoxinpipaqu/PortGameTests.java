package com.shaoxinpipaqu;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;
import java.util.function.Consumer;

@Mod(Pipa.ID)
public final class PortGameTests {
    private record Test(String name,int ticks,Consumer<GameTestHelper> run) {}
    private static final List<Test> TESTS=List.of(
            new Test("remoteandflight",360,PortRegression::remoteAndFlight),
            new Test("landingoverlap",100,PortRegression::landingOverlap),
            new Test("meals",100,PortRegression::meals),
            new Test("bowltrade",100,PortRegression::bowlTrade),
            new Test("peace",100,PortRegression::peace));

    public PortGameTests(IEventBus bus) {
        var functions=DeferredRegister.<Consumer<GameTestHelper>>create(Registries.TEST_FUNCTION,Pipa.ID);
        for(var test:TESTS)functions.register(test.name(),test::run);
        functions.register(bus);
        bus.addListener((RegisterGameTestsEvent event)->{
            for(var test:TESTS) {
                var id=Identifier.fromNamespaceAndPath(Pipa.ID,test.name());
                var env=event.registerEnvironment(id,new TestEnvironmentDefinition.AllOf(List.of()));
                var data=new TestData<>(env,Identifier.fromNamespaceAndPath(Pipa.ID,"range"),test.ticks(),0,true);
                event.registerTest(id,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id),data));
            }
        });
    }
}
