package net.ultrahex.mc.debugutils.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RecalcLightingCommand extends CommandBase {

    final Map<World, Worker> workers = new ConcurrentHashMap<>();

    @Override
    public String getCommandName() {
        return "recalcLighting";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "debugutils:commands.recalcLighting.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 0) {
            throw new CommandException("debugutils:commands.recalcLighting.usage");
        }

        WorldClient world = Minecraft.getMinecraft().theWorld;

        IntegratedServer server = Minecraft.getMinecraft()
            .getIntegratedServer();
        if (server == null) {
            return;
        }

        IChunkProvider chunkProvider = server.getEntityWorld()
            .getChunkProvider();
        if (!(chunkProvider instanceof ChunkProviderServer)) {
            return;
        }

        if (workers.containsKey(world)) {
            sender.addChatMessage(new ChatComponentText("Starting over."));
            workers.get(world).stopping = true;
        } else {
            sender.addChatMessage(new ChatComponentText("Hold onto your butts!"));
        }

        @SuppressWarnings("unchecked")
        List<Chunk> loadedChunks = new ArrayList<Chunk>(((ChunkProviderServer) chunkProvider).loadedChunks);

        Worker worker = new Worker(world, loadedChunks);
        workers.put(world, worker);
        worker.start();
    }

    class Worker extends Thread {

        private final List<Chunk> chunks;
        private final World world;
        private boolean stopping = false;

        public Worker(World world, List<Chunk> chunks) {
            this.chunks = chunks;
            this.world = world;
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            if (world == event.world) stopping = true;
        }

        @Override
        public void run() {
            MinecraftForge.EVENT_BUS.register(this);

            for (Chunk chunk : chunks) {
                for (int i = 0; i < 4096; ++i) {
                    int yPosition = i % 16;
                    int localX = i / 16 % 16;
                    int localZ = i / 256;
                    int x = (chunk.xPosition << 4) + localX;
                    int z = (chunk.zPosition << 4) + localZ;

                    for (int localY = 0; localY < 16; ++localY) {
                        int y = (yPosition << 4) + localY;

                        if (chunk.getBlockStorageArray()[yPosition] == null
                            && (localY == 0 || localY == 15
                                || localX == 0
                                || localX == 15
                                || localZ == 0
                                || localZ == 15)
                            || chunk.getBlockStorageArray()[yPosition] != null
                                && chunk.getBlockStorageArray()[yPosition].getBlockByExtId(localX, localY, localZ)
                                    .getMaterial() == Material.air) {
                            if (world.getBlock(x, y - 1, z)
                                .getLightValue() > 0) {
                                world.func_147451_t(x, y - 1, z);
                            }

                            if (world.getBlock(x, y + 1, z)
                                .getLightValue() > 0) {
                                world.func_147451_t(x, y + 1, z);
                            }

                            if (world.getBlock(x - 1, y, z)
                                .getLightValue() > 0) {
                                world.func_147451_t(x - 1, y, z);
                            }

                            if (world.getBlock(x + 1, y, z)
                                .getLightValue() > 0) {
                                world.func_147451_t(x + 1, y, z);
                            }

                            if (world.getBlock(x, y, z - 1)
                                .getLightValue() > 0) {
                                world.func_147451_t(x, y, z - 1);
                            }

                            if (world.getBlock(x, y, z + 1)
                                .getLightValue() > 0) {
                                world.func_147451_t(x, y, z + 1);
                            }

                            world.func_147451_t(x, y, z);
                        }
                    }
                }

                if (stopping) {
                    break;
                }
            }

            workers.remove(world, this);

            MinecraftForge.EVENT_BUS.unregister(this);
            if (!stopping) {
                Minecraft.getMinecraft().thePlayer
                    .addChatMessage(new ChatComponentText("Lighting recalculation complete!"));
            }
        }
    }
}
