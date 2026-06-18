package net.yigitguven.claim.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yigitguven.claim.core.ClientClaimManager;
import net.yigitguven.claim.network.ClaimSyncPayload;
import net.yigitguven.claim.network.OpenClaimListPayload;

public class ClientPayloadHandler
{
    public static void handleSync(final ClaimSyncPayload payload, final IPayloadContext context)
    {
        System.out.println("[Claim] Received sync payload with " + payload.claims().size() + " claims.");
        context.enqueueWork(() -> {
            ClientClaimManager.setClaims(payload.claims());
            System.out.println("[Claim] Claims updated in ClientClaimManager. Refreshing Xaero Map...");
            net.yigitguven.claim.integration.XaeroMapIntegration.refresh();
        });
    }

    public static void handleOpenList(final OpenClaimListPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(new ClaimListScreen());
        });
    }
}