/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.util;

import static cubicchunks.util.Coords.blockToLocal;

import cubicchunks.client.CubeProviderClient;
import cubicchunks.lighting.ILightBlockAccess;
import cubicchunks.server.CubeProviderServer;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.SidedProxy;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Simple class that allows to quickly access blocks near specified cube without the overhead of getting these cubes.
 * <p>
 * Does not allow to set blocks, only get blocks, their opacity and get/set light values.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FastCubeBlockAccess implements ILightBlockAccess {

    @SidedProxy private static GetLoadedChunksProxy getLoadedChunksProxy;
    @Nonnull private final Cube[][][] cache;
    private final int originX, originY, originZ;
    @Nonnull private final ICubicWorld world;

    public FastCubeBlockAccess(ICubeProvider cache, Cube cube, int radius) {
        this(cube.getCubicWorld(), cache,
                cube.getCoords().sub(radius, radius, radius), cube.getCoords().add(radius, radius, radius));
    }

    private FastCubeBlockAccess(ICubicWorld world, ICubeProvider prov, CubePos start, CubePos end) {
        int dx = Math.abs(end.getX() - start.getX()) + 1;
        int dy = Math.abs(end.getY() - start.getY()) + 1;
        int dz = Math.abs(end.getZ() - start.getZ()) + 1;

        this.world = world;
        this.cache = new Cube[dx][dy][dz];
        this.originX = Math.min(start.getX(), end.getX());
        this.originY = Math.min(start.getY(), end.getY());
        this.originZ = Math.min(start.getZ(), end.getZ());

        for (int relativeCubeX = 0; relativeCubeX < dx; relativeCubeX++) {
            for (int relativeCubeZ = 0; relativeCubeZ < dz; relativeCubeZ++) {
                for (int relativeCubeY = 0; relativeCubeY < dy; relativeCubeY++) {
                    this.cache[relativeCubeX][relativeCubeY][relativeCubeZ] =
                            prov.getLoadedCube(originX + relativeCubeX, originY + relativeCubeY, originZ + relativeCubeZ);
                    if (this.cache[relativeCubeX][relativeCubeY][relativeCubeZ] == null) {
                        CrashReport report = CrashReport.makeCrashReport(
                                new IllegalStateException("Cube not loaded"), "Creating cube cache");
                        CrashReportCategory category = report.makeCategory("ILightBlockAccess");

                        CubePos pos = new CubePos(originX + relativeCubeX, originY + relativeCubeY, originZ + relativeCubeZ);
                        category.setDetail("Getting cube", pos::toString);
                        if (prov instanceof CubeProviderServer || prov instanceof CubeProviderClient) {
                            Iterable<Chunk> chunks = getLoadedChunksProxy.getLoadedChunks(prov);
                            int i = 0;
                            for (Chunk chunk : chunks) {
                                Column column = (Column) chunk;
                                category.setDetail("Column" + i, () ->
                                        column.getLoadedCubes().stream().map(
                                                c -> c.getCoords().toString()
                                        ).reduce((a, b) -> a + ", " + b).orElse(null)
                                );
                            }
                        }

                        throw new ReportedException(report);
                    }
                }
            }
        }
    }

    private Cube getCube(int blockX, int blockY, int blockZ) {
        int cubeX = Coords.blockToCube(blockX);
        int cubeY = Coords.blockToCube(blockY);
        int cubeZ = Coords.blockToCube(blockZ);

        return this.cache[cubeX - originX][cubeY - originY][cubeZ - originZ];
    }

    private IBlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    private IBlockState getBlockState(int blockX, int blockY, int blockZ) {
        return this.getCube(blockX, blockY, blockZ).getBlockState(blockX, blockY, blockZ);
    }

    @Override public int getBlockLightOpacity(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ()).getLightOpacity((World) world, pos);
    }

    @Override public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
        return this.getCube(pos.getX(), pos.getY(), pos.getZ()).getLightFor(lightType, pos);
    }

    @Override public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int val) {
        this.getCube(pos.getX(), pos.getY(), pos.getZ()).setLightFor(lightType, pos, val);
    }

    @Override public boolean canSeeSky(BlockPos pos) {
        Cube cube = getCube(pos.getX(), pos.getY(), pos.getZ());
        Column column = cube.getColumn();
        int height = column.getHeightValue(blockToLocal(pos.getX()), blockToLocal(pos.getZ()));
        return height <= pos.getY();
    }

    @Override public int getEmittedLight(BlockPos pos, EnumSkyBlock type) {
        switch (type) {
            case BLOCK:
                return getBlockState(pos).getLightValue((IBlockAccess) world, pos);
            case SKY:
                return canSeeSky(pos) ? 15 : 0;
            default:
                throw new AssertionError();
        }
    }

    public static ILightBlockAccess forBlockRegion(ICubeProvider prov, BlockPos startPos, BlockPos endPos) {
        //TODO: fix it
        BlockPos midPos = Coords.midPos(startPos, endPos);
        Cube center = prov.getCube(CubePos.fromBlockCoords(midPos));
        return new FastCubeBlockAccess(center.getCubicWorld(), prov,
                CubePos.fromBlockCoords(startPos), CubePos.fromBlockCoords(endPos));
    }

    private interface GetLoadedChunksProxy {

        Iterable<Chunk> getLoadedChunks(ICubeProvider prov);
    }

    public static final class ServerProxy implements GetLoadedChunksProxy {

        public ServerProxy() {
        }

        @Override public Iterable<Chunk> getLoadedChunks(ICubeProvider prov) {
            return ((CubeProviderServer) prov).getLoadedChunks();
        }
    }

    public static final class ClientProxy implements GetLoadedChunksProxy {

        public ClientProxy() {
        }

        @Override public Iterable<Chunk> getLoadedChunks(ICubeProvider prov) {
            return ((CubeProviderClient) prov).getLoadedChunks();
        }
    }
}
