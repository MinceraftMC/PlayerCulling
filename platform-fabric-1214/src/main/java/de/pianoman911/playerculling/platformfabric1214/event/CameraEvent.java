package de.pianoman911.playerculling.platformfabric1214.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

public final class CameraEvent {

    public static final Event<CameraEvent.StartStopSpectating> START_STOP_SPECTATING = EventFactory.createArrayBacked(CameraEvent.StartStopSpectating.class, callbacks -> (player, start) -> {
        for (CameraEvent.StartStopSpectating callback : callbacks) {
            callback.onStartStopSpectating(player, start);
        }
    });

    private CameraEvent() {
    }

    public interface StartStopSpectating {

        void onStartStopSpectating(ServerPlayer player, boolean start);
    }
}
