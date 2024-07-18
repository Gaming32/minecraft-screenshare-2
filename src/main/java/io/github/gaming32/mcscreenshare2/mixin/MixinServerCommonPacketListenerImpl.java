package io.github.gaming32.mcscreenshare2.mixin;

import io.github.gaming32.mcscreenshare2.ext.ServerCommonPacketListenerImplExt;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class MixinServerCommonPacketListenerImpl implements ServerCommonPacketListenerImplExt {
    @Unique
    private final Int2LongMap mcscreenshare2$lastPings = new Int2LongOpenHashMap();

    @Unique
    private long mcscreenshare2$currentPing = 200L;

    @Unique
    private long mcscreenshare2$lastFrameSend = 0L;

    @Unique
    private boolean mcscreenshare2$shouldFrameBeSent = true;

    @Override
    public Int2LongMap mcscreenshare2$getLastPings() {
        return mcscreenshare2$lastPings;
    }

    @Override
    public void mcscreenshare2$updateShouldFrameBeSent() {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - mcscreenshare2$lastFrameSend < mcscreenshare2$currentPing) {
            mcscreenshare2$shouldFrameBeSent = false;
        } else {
            mcscreenshare2$lastFrameSend = currentTime;
            mcscreenshare2$shouldFrameBeSent = true;
        }
    }

    @Override
    public boolean mcscreenshare2$shouldFrameBeSent() {
        return mcscreenshare2$shouldFrameBeSent;
    }

    @Inject(method = "handlePong", at = @At("HEAD"), cancellable = true)
    private void onPong(ServerboundPongPacket serverboundPongPacket, CallbackInfo ci) {
        final long currentTime = System.currentTimeMillis();
        final long sendTime = mcscreenshare2$lastPings.remove(serverboundPongPacket.getId());
        if (sendTime == 0L) return;
        ci.cancel();
        mcscreenshare2$currentPing = currentTime - sendTime;
    }
}
