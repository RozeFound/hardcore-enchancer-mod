package com.github.RozeFound.hardcore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.stat.Stats;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;

@Mixin(value = ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Inject(method = "onClientStatus(Lnet/minecraft/network/packet/c2s/play/ClientStatusC2SPacket;)V", at = @At("TAIL"))
    public void onClientStatus(ClientStatusC2SPacket packet, CallbackInfo ci) {

        ClientStatusC2SPacket.Mode mode = packet.getMode();
        var player = ((ServerPlayNetworkHandler)(Object)this).player;
        var deaths = player.getStatHandler().getStat(Stats.CUSTOM, Stats.DEATHS);

        if (mode == ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) {

            if (deaths < 10) {

                var spawn_coords = player.getServer().getWorld(World.OVERWORLD).getSpawnPos();
                player.teleport(spawn_coords.getX(), spawn_coords.getY(), spawn_coords.getZ());
                player.changeGameMode(GameMode.SURVIVAL);

                var attribute = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
                attribute.setBaseValue( 20 - (deaths * 2));
                
            }
        
        }
        
    }
    
}
