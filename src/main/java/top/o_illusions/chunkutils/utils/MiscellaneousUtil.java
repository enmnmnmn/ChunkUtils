package top.o_illusions.chunkutils.utils;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.function.Predicate;

public class MiscellaneousUtil {
    public MiscellaneousUtil() {
    }

    public static RegistryEntry<Biome> getOriginalBiome(ServerWorld world, BlockPos pos)
    {
        ServerChunkManager chunkManager = world.getChunkManager();
        ChunkGenerator chunkGenerator = chunkManager.getChunkGenerator();
        BiomeSource biomeSource = chunkGenerator.getBiomeSource();
        MultiNoiseUtil.MultiNoiseSampler Sampler = chunkManager.getNoiseConfig().getMultiNoiseSampler();

        int biomeX = BiomeCoords.fromBlock(pos.getX());
        int biomeY = BiomeCoords.fromBlock(pos.getY());
        int biomeZ = BiomeCoords.fromBlock(pos.getZ());


        return biomeSource.getBiome(biomeX, biomeY, biomeZ, Sampler);
    }

    public static BiomeSupplier createBiomeSupplier(MutableInt counter, Chunk chunk, BlockBox box, RegistryEntry<Biome> biome, Predicate<RegistryEntry<Biome>> filter) {
        return (x, y, z, noise) -> {
            int i = BiomeCoords.toBlock(x);
            int j = BiomeCoords.toBlock(y);
            int k = BiomeCoords.toBlock(z);
            RegistryEntry<Biome> registryEntry2 = chunk.getBiomeForNoiseGen(x, y, z);
            if (box.contains(i, j, k) && filter.test(registryEntry2)) {
                counter.increment();
                return biome;
            } else {
                return registryEntry2;
            }
        };
    }
}
