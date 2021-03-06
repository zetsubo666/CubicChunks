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
package cubicchunks.world;

import com.google.common.base.Throwables;
import cubicchunks.util.Coords;
import cubicchunks.world.column.Column;
import mcp.MethodsReturnNonnullByDefault;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ClientHeightMap implements IHeightMap {

    @Nonnull private int[] hmap;
    private int heightMapLowest = Coords.NO_HEIGHT;

    public ClientHeightMap(Column column) {
        this.hmap = new int[256];

        Arrays.fill(hmap, Coords.NO_HEIGHT);
    }

    @Override
    public boolean isOccluded(int localX, int blockY, int localZ) {
        int topY = this.getTopBlockY(localX, localZ);
        return blockY <= topY;
    }

    @Override
    public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
        //do nothing, we return values based on real blocks
    }

    @Override
    public int getTopBlockY(int localX, int localZ) {
        return hmap[getIndex(localX, localZ)];
    }

    @Override
    public int getLowestTopBlockY() {
        if (heightMapLowest == Coords.NO_HEIGHT) {
            heightMapLowest = Integer.MAX_VALUE;
            for (int i = 0; i < hmap.length; i++) {
                if (hmap[i] < heightMapLowest) {
                    heightMapLowest = hmap[i];
                }
            }
        }
        return heightMapLowest;
    }

    @Override
    public int getTopBlockYBelow(int localX, int localZ, int blockY) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setHeight(int localX, int localZ, int height) {
        hmap[getIndex(localX, localZ)] = height;
    }

    public void setData(@Nonnull byte[] data) {
        try {
            ByteArrayInputStream buf = new ByteArrayInputStream(data);
            DataInputStream in = new DataInputStream(buf);

            for (int i = 0; i < 256; i++) {
                hmap[i] = in.readInt();
            }

            in.close();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public int[] getHeightmap() {
        return this.hmap;
    }

    private static int getIndex(int localX, int localZ) {
        return (localZ << 4) | localX;
    }
}
