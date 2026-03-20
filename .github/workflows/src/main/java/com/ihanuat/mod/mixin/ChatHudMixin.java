package com.ihanuat.mod.mixin;

import com.ihanuat.mod.MacroConfig;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ChatHudMixin — handles chat cleanup filter only.
 * ChatRules are handled via ClientReceiveMessageEvents in IhanuatClient,
 * which is the correct Fabric API approach for MC 1.21.x and avoids
 * brittle mixin signature matching across minor versions.
 */
@Mixin(ChatComponent.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Component message, CallbackInfo ci) {
        if (!MacroConfig.hideFilteredChat) {
            return;
        }
        String rawText = message.getString();
        if (rawText.contains("for killing a") ||
                rawText.contains("Starting script via command")) {
            ci.cancel();
        }
    }
}
