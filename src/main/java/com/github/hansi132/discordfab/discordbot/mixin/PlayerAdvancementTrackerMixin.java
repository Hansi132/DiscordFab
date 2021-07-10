package com.github.hansi132.discordfab.discordbot.mixin;

import com.github.hansi132.discordfab.DiscordFab;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.kilocraft.essentials.api.KiloEssentials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementTrackerMixin {

    @Shadow private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcastChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
    private void discordFab$OnAdvancement(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        AdvancementDisplay display = advancement.getDisplay();
        if (display == null) return;
        AdvancementFrame frame = display.getFrame();
        Text message = new TranslatableText("chat.type.advancement." + frame.getId(), this.owner.getDisplayName(), advancement.toHoverableText());
        DiscordFab.getInstance().getChatSynchronizer().broadCastPlayerEvent(KiloEssentials.getServer().getOnlineUser(owner), message.getString(), frame == AdvancementFrame.CHALLENGE ? new Color(137, 50, 183) : new Color(128, 199, 31));
    }

}
