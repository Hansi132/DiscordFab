package com.github.hansi132.discordfab.discordbot.mixin;

import com.github.hansi132.discordfab.DiscordFab;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.kilocraft.essentials.api.KiloEssentials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.*;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Redirect(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageTracker;getDeathMessage()Lnet/minecraft/text/Text;"))
    private Text discordFab$OnDeath(DamageTracker tracker) {
        final Text text = tracker.getDeathMessage();
        DiscordFab.getInstance().getChatSynchronizer().broadCastPlayerEvent(KiloEssentials.getServer().getOnlineUser((ServerPlayerEntity) (Object)this), text.getString(), new Color(156, 157, 151));
        return text;
    }

}
