package io.github.gaming32.mcscreenshare2.ext;

import it.unimi.dsi.fastutil.ints.Int2LongMap;

public interface ServerCommonPacketListenerImplExt {
    Int2LongMap mcscreenshare2$getLastPings();

    void mcscreenshare2$updateShouldFrameBeSent();

    boolean mcscreenshare2$shouldFrameBeSent();
}
