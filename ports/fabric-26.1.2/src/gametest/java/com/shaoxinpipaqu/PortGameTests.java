package com.shaoxinpipaqu;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class PortGameTests {
    @GameTest(structure="shaoxinpipaqu:range",environment="shaoxinpipaqu-tests:empty",maxTicks=360)
    public void remoteAndFlight(GameTestHelper helper) { PortRegression.remoteAndFlight(helper); }

    @GameTest(structure="shaoxinpipaqu:range",environment="shaoxinpipaqu-tests:empty",maxTicks=100)
    public void landingOverlap(GameTestHelper helper) { PortRegression.landingOverlap(helper); }

    @GameTest(structure="shaoxinpipaqu:range",environment="shaoxinpipaqu-tests:empty",maxTicks=100)
    public void meals(GameTestHelper helper) { PortRegression.meals(helper); }

    @GameTest(structure="shaoxinpipaqu:range",environment="shaoxinpipaqu-tests:empty",maxTicks=100)
    public void bowlTrade(GameTestHelper helper) { PortRegression.bowlTrade(helper); }

    @GameTest(structure="shaoxinpipaqu:range",environment="shaoxinpipaqu-tests:empty",maxTicks=100)
    public void peace(GameTestHelper helper) { PortRegression.peace(helper); }
}
