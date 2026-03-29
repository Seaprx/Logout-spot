
package com.example.logoutspots;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class LogoutSpotsMod implements ClientModInitializer {

    private static final Map<UUID, LogoutSpot> logoutSpots = new HashMap<>();
    private static final Set<UUID> knownPlayers = new HashSet<>();
    private static boolean enabled = true;

    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Toggle Logout Spots",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "Logout Spots"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.getNetworkHandler() == null) return;

            while (toggleKey.wasPressed()) {
                enabled = !enabled;
            }

            if (!enabled) return;

            Collection<PlayerListEntry> playerList = client.getNetworkHandler().getPlayerList();
            Set<UUID> currentPlayers = new HashSet<>();

            for (PlayerListEntry entry : playerList) {
                currentPlayers.add(entry.getProfile().getId());
            }

            for (UUID uuid : new HashSet<>(knownPlayers)) {
                if (!currentPlayers.contains(uuid)) {
                    PlayerEntity player = client.world.getPlayerByUuid(uuid);
                    if (player != null) {
                        logoutSpots.put(uuid, new LogoutSpot(
                                player.getName().getString(),
                                player.getPos()
                        ));
                    }
                }
            }

            knownPlayers.clear();
            knownPlayers.addAll(currentPlayers);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!enabled) return;

            MinecraftClient client = MinecraftClient.getInstance();
            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();

            for (LogoutSpot spot : logoutSpots.values()) {
                renderBox(matrices, spot.pos);
                renderName(matrices, consumers, spot);
            }
        });
    }

    private void renderBox(MatrixStack matrices, Vec3d pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getPos();

        Box box = new Box(
                pos.x - 0.3, pos.y, pos.z - 0.3,
                pos.x + 0.3, pos.y + 1.8, pos.z + 0.3
        ).offset(-camera.x, -camera.y, -camera.z);

        net.minecraft.client.render.WorldRenderer.drawBox(
                matrices,
                net.minecraft.client.render.Tessellator.getInstance().getBuffer(),
                box,
                1.0f, 0.0f, 1.0f,
                1.0f
        );
    }

    private void renderName(MatrixStack matrices, VertexConsumerProvider consumers, LogoutSpot spot) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getPos();

        matrices.push();

        matrices.translate(
                spot.pos.x - camera.x,
                spot.pos.y + 2.2 - camera.y,
                spot.pos.z - camera.z
        );

        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);

        client.textRenderer.draw(
                spot.name,
                -client.textRenderer.getWidth(spot.name) / 2f,
                0,
                0xFFFFFF,
                false,
                matrices.peek().getPositionMatrix(),
                consumers,
                net.minecraft.client.render.TextRenderer.TextLayerType.NORMAL,
                0,
                15728880
        );

        matrices.pop();
    }

    private static class LogoutSpot {
        String name;
        Vec3d pos;

        LogoutSpot(String name, Vec3d pos) {
            this.name = name;
            this.pos = pos;
        }
    }
}
