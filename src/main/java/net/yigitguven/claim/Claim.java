package net.yigitguven.claim;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.yigitguven.claim.commands.ClaimCommand;
import net.yigitguven.claim.core.ClaimManager;
import net.yigitguven.claim.network.ClaimSyncPayload;
import net.yigitguven.claim.network.PayloadHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

@Mod(Claim.MODID)
public class Claim
{
    public static final String MODID = "claim";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Claim(IEventBus modEventBus, ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.COMMON, net.yigitguven.claim.config.ModConfig.SPEC);
        
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::clientSetup);
        
        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event)
    {
        LOGGER.info("Registering payloads for mod: {}", MODID);
        final PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0");

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            registrar.playToClient(
                    ClaimSyncPayload.TYPE,
                    ClaimSyncPayload.STREAM_CODEC,
                    net.yigitguven.claim.client.ClientPayloadHandler::handleSync
            );
            registrar.playToClient(
                    net.yigitguven.claim.network.OpenClaimListPayload.TYPE,
                    net.yigitguven.claim.network.OpenClaimListPayload.CODEC,
                    net.yigitguven.claim.client.ClientPayloadHandler::handleOpenList
            );
        } else {
            registrar.playToClient(ClaimSyncPayload.TYPE, ClaimSyncPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(net.yigitguven.claim.network.OpenClaimListPayload.TYPE, net.yigitguven.claim.network.OpenClaimListPayload.CODEC, (p, c) -> {});
        }

        registrar.playToServer(
                net.yigitguven.claim.network.RequestClaimPayload.TYPE,
                net.yigitguven.claim.network.RequestClaimPayload.STREAM_CODEC,
                PayloadHandler::handleRequestClaim
        );
        registrar.playToServer(
                net.yigitguven.claim.network.RequestUnclaimPayload.TYPE,
                net.yigitguven.claim.network.RequestUnclaimPayload.STREAM_CODEC,
                PayloadHandler::handleRequestUnclaim
        );
        registrar.playToServer(
                net.yigitguven.claim.network.UpdateClaimMetadataPayload.TYPE,
                net.yigitguven.claim.network.UpdateClaimMetadataPayload.STREAM_CODEC,
                PayloadHandler::handleUpdateMetadata
        );
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        // Integration is handled via Mixin
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Claim mod server starting, loading claims...");
        ClaimManager.load(event.getServer().overworld());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        LOGGER.info("Claim mod server stopping, saving claims...");
        ClaimManager.save(event.getServer().overworld());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            syncClaims(player);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClaimCommand.register(event.getDispatcher());
    }

    public static void syncClaims(ServerPlayer player)
    {
        player.connection.send(new ClaimSyncPayload(ClaimManager.getClaims()));
    }
}
