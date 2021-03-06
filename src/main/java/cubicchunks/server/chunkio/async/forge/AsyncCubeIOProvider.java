/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cubicchunks.server.chunkio.async.forge;

import cubicchunks.CubicChunks;
import cubicchunks.server.chunkio.ICubeIO;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Async loading of cubes
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class AsyncCubeIOProvider extends AsyncIOProvider<Cube> {

    @Nonnull private final QueuedCube cubeInfo;
    @Nonnull private final ICubeIO loader;

    @Nonnull private CompletableFuture<Column> futureColumn = new CompletableFuture<>();
    @Nullable private ICubeIO.PartialCubeData cubeData;

    AsyncCubeIOProvider(QueuedCube cube, ICubeIO loader) {
        this.cubeInfo = cube;
        this.loader = loader;
    }

    @Override
    public synchronized void run() {
        try {
            cubeData = this.loader.loadCubeAsyncPart(futureColumn.get(), this.cubeInfo.y);
        } catch (IOException e) {
            CubicChunks.LOGGER
                    .error("Could not load cube in {} @ ({}, {}, {})", this.cubeInfo.world, this.cubeInfo.x, this.cubeInfo.y, this.cubeInfo.z, e);
        } catch (InterruptedException e) {
            throw new Error(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            this.finished = true;
            this.notifyAll();
        }
    }

    // sync stuff
    @Override
    public void runSynchronousPart() {

        if (cubeData != null) {
            this.loader.loadCubeSyncPart(cubeData);
        }
        // TBD:
        // this.provider.cubeGenerator.recreateStructures(this.cube, this.cubeInfo.x, this.cubeInfo.z);

        this.runCallbacks();
    }

    @Nullable @Override
    public Cube get() {
        return cubeData == null ? null : cubeData.getCube();
    }

    public void setColumn(@Nullable Column column) {
        this.futureColumn.complete(column);
    }
}
