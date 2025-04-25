package top.o_illusions.chunkutils;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import top.o_illusions.chunkutils.command.ResetBiomeCommand;
import top.o_illusions.chunkutils.command.FillBiomeChunk;


public class ChunkUtils implements ModInitializer {

    @Override
    public void onInitialize() {
        FillBiomeChunk worldSync = new FillBiomeChunk();
        ResetBiomeCommand resetBiome = new ResetBiomeCommand();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            worldSync.register(dispatcher, registryAccess);
            resetBiome.register(dispatcher, registryAccess);
        });

    }
}


