package net.yigitguven.claim.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PayloadHandler
{
    public static void handleRequestClaim(final net.yigitguven.claim.network.RequestClaimPayload payload, final IPayloadContext context)
    {
        System.out.println("[Claim] Received request claim payload from " + context.player().getName().getString());
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player)
            {
                int maxClaims = net.yigitguven.claim.config.ModConfig.MAX_CLAIMS.get();
                long maxBlocksPerClaim = net.yigitguven.claim.config.ModConfig.MAX_BLOCKS_PER_CLAIM.get();
                long maxTotalBlocks = net.yigitguven.claim.config.ModConfig.MAX_TOTAL_BLOCKS.get();

                boolean isOp = player.hasPermissions(2);
                boolean bypass = isOp && net.yigitguven.claim.config.ModConfig.OP_BYPASS.get();

                if (!bypass)
                {
                    if (maxClaims != -1 && net.yigitguven.claim.core.ClaimManager.getClaimCount(player.getUUID()) >= maxClaims)
                    {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.claim.max_claims", maxClaims), false);
                        return;
                    }

                    long newClaimBlocks = net.yigitguven.claim.core.ClaimManager.calculateVolume(player.serverLevel(), payload.pos1(), payload.pos2());
                    if (maxBlocksPerClaim != -1 && newClaimBlocks > maxBlocksPerClaim)
                    {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.claim.max_blocks_per_claim", maxBlocksPerClaim, newClaimBlocks), false);
                        return;
                    }

                    long currentTotal = net.yigitguven.claim.core.ClaimManager.getTotalClaimedBlocks(player.getUUID());
                    if (maxTotalBlocks != -1 && (currentTotal + newClaimBlocks) > maxTotalBlocks)
                    {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.claim.max_total_blocks", maxTotalBlocks, currentTotal, newClaimBlocks), false);
                        return;
                    }

                    if (!net.yigitguven.claim.core.ClaimManager.hasRequiredLandPermit(player))
                    {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.claim.permit_required", net.yigitguven.claim.config.ModConfig.LAND_PERMIT_AMOUNT.get(), net.yigitguven.claim.config.ModConfig.LAND_PERMIT_ITEM.get()), false);
                        return;
                    }
                }

                System.out.println("[Claim] Adding claim for " + player.getName().getString() + " at " + payload.pos1() + " to " + payload.pos2());
                if (net.yigitguven.claim.core.ClaimManager.addClaim(player.serverLevel(), player.getName().getString() + "'s Claim", player.getUUID(), payload.pos1(), payload.pos2()) && !bypass)
                {
                    net.yigitguven.claim.core.ClaimManager.consumeLandPermit(player);
                }
            }
        });
    }

    public static void handleRequestUnclaim(final net.yigitguven.claim.network.RequestUnclaimPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player)
            {
                net.yigitguven.claim.core.ClaimManager.removeClaims(player.serverLevel(), payload.claimIds(), player.getUUID());
            }
        });
    }

    public static void handleUpdateMetadata(final UpdateClaimMetadataPayload payload, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player)
            {
                boolean isOp = player.hasPermissions(2);
                boolean bypass = isOp && net.yigitguven.claim.config.ModConfig.OP_BYPASS.get();
                
                String name = payload.name();
                String description = payload.description();
                net.yigitguven.claim.core.ClaimData.PermissionMode mode = payload.mode();
                int color = payload.color();
                java.util.List<java.util.UUID> trustedPlayers = payload.trustedPlayers();

                // If locked and not bypass, use current values from the server instead of payload
                // We need to find the claim first
                net.yigitguven.claim.core.ClaimData existing = net.yigitguven.claim.core.ClaimManager.getClaims().stream()
                        .filter(c -> c.claimId == payload.claimId())
                        .findFirst().orElse(null);

                if (existing != null && !bypass)
                {
                    if (net.yigitguven.claim.config.ModConfig.LOCK_CLAIM_NAME.get()) name = existing.displayName;
                    if (net.yigitguven.claim.config.ModConfig.LOCK_CLAIM_DESCRIPTION.get()) description = existing.description;
                    if (net.yigitguven.claim.config.ModConfig.LOCK_CLAIM_PERMISSIONS.get()) mode = existing.permissionMode;
                    if (net.yigitguven.claim.config.ModConfig.LOCK_CLAIM_COLOR.get()) color = existing.color;
                    if (net.yigitguven.claim.config.ModConfig.LOCK_CLAIM_TRUSTED.get()) trustedPlayers = existing.trustedPlayers;
                }

                net.yigitguven.claim.core.ClaimManager.updateClaimMetadata(player.serverLevel(), 
                    payload.claimId(), name, description, 
                    mode, color, trustedPlayers, player.getUUID());
            }
        });
    }
}
