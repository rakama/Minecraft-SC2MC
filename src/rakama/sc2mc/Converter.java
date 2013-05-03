/*
 * Copyright (c) 2012, RamsesA <ramsesakama@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

package rakama.sc2mc;

import java.util.Random;

import rakama.sc2mc.map.SC2Map;
import rakama.sc2mc.map.StructureMap;
import rakama.sc2mc.map.TerrainMap;

import rakama.worldtools.canvas.BlockCanvas;
import rakama.worldtools.data.Biome;
import rakama.worldtools.data.Block;

public class Converter
{
    protected static final int width = 128;
    protected static final int height = 128;
    protected static final int grid_scale = 16;
    
    protected Random rand;
    protected BlockCanvas canvas;
    protected SC2Map map;
    
    public Converter(SC2Map map, BlockCanvas canvas)
    {
        this.map = map;
        this.canvas = canvas;
        this.rand = new Random(0);
    }
    
    public void convert(boolean verbose)
    {
        for(int y=0; y<height; y++)
        {
            for(int x=0; x<width; x++)
                renderChunk(x, y);
            
            if(verbose)
                log("Generating... " + getPercentage(y + 1) + "% complete");            
        }
    }
    
    protected void renderChunk(int x0, int y0)
    {
        TerrainMap terra = map.getTerrainMap();
        
        // generate terrain
        renderTerrain(x0, y0);
        
        // generate waterfalls
        if(terra.isWaterfall(x0, y0))
            renderWaterfall(x0, y0);
        
        StructureMap struct = map.getStructureMap();

        // generate shrub
        if(struct.isEmptyLot(x0, y0))
            renderEmptyLot(x0, y0);
        
        // generate roads
        if(struct.isRoad(x0, y0))
            renderRoad(x0, y0);

        // generate highway
        if(struct.isHighway(x0, y0))
            renderHighway(x0, y0);

        // generate rail
        if(struct.isRail(x0, y0))
            renderRail(x0, y0);

        // generate powerline
        if(struct.isPowerline(x0, y0))
            renderPowerline(x0, y0);
        
        // generate trees
        int numTrees = (int)(struct.getTreeDensity(x0, y0) * 1.5);        
        for(int i=0; i<numTrees; i++)
            renderTree(x0, y0);
    }
    
    protected void renderTerrain(int x0, int y0)
    {
        int xStart = getScaledCoordinate(x0, width);
        int yStart = getScaledCoordinate(y0, height);
        int xEnd = xStart + grid_scale;
        int yEnd = yStart + grid_scale;
                                
        // generate terrain
        for(int y=yStart; y<yEnd; y++)
            for(int x=xStart; x<xEnd; x++)
                renderColumn(x, y, getScaledAltitude(x, y), getScaledWaterAltitude(x, y));
    }

    protected void renderEmptyLot(int x0, int y0)
    {
        if(map.getTerrainMap().isFlooded(x0, y0) || rand.nextDouble() > 0.02)
            return;

        int x = getScaledCoordinate(x0, width) + rand.nextInt(grid_scale);
        int y = getScaledCoordinate(y0, height) + rand.nextInt(grid_scale);
        
        canvas.setBlock(x, getScaledAltitude(x, y), y, Block.SHRUB);
    }
    
    protected void renderWaterfall(int x0, int y0)
    {
        int xStart = getScaledCoordinate(x0, width);
        int yStart = getScaledCoordinate(y0, height);
        int xEnd = xStart + grid_scale;
        int yEnd = yStart + grid_scale;
        int altStart = map.getTerrainMap().getTerrainAltitude(x0, y0) * grid_scale - 1;
        int altEnd = altStart + grid_scale;
                
        for(int y=yStart; y<yEnd; y++)
        {
            for(int x=xStart; x<xEnd; x++)
            {
                Block block = Block.WATER;                
                if(x == xStart || x == xEnd - 1 || y == yStart || y == yEnd - 1)
                    block = Block.getBlock(9, 8);
                
                for(int alt=altStart; alt<=altEnd; alt++)
                    canvas.setBlock(x, alt, y, block);
            }
        }
    }

    protected void renderTree(int x0, int y0)
    {                   
        int x = getScaledCoordinate(x0, width) + rand.nextInt(grid_scale);
        int y = getScaledCoordinate(y0, height) + rand.nextInt(grid_scale);
        int height = 6 + rand.nextInt(4);
        
        int altitude = getScaledAltitude(x, y);
        
        // leaves around trunk
        for(int i=2; i<height; i++)
            for(int j=0; j<3; j++)
                for(int k=0; k<3; k++)
                    if((i & 1) == 0 || (j & 1) != (k & 1))
                        canvas.setBlock(x + j - 1, altitude + i, y + k - 1, Block.getBlock(18));

        // leaves at top
        canvas.setBlock(x, altitude + height, y, Block.getBlock(18));
        
        // trunk
        for(int i=0; i<height; i++)
            canvas.setBlock(x, altitude + i, y, Block.WOOD);

        // dirt underneath (unless on slope)
        if(isBuried(x, altitude - 1, y))
            canvas.setBlock(x, altitude - 1, y, Block.DIRT);        
    }

    protected void renderRail(int x0, int y0)
    {
        // TODO: not yet implemented                
    }

    protected void renderPowerline(int x0, int y0)
    {
        // TODO: not yet implemented
    }
    
    protected void renderRoad(int x0, int y0)
    {
        // TODO: not yet implemented

        int xStart = getScaledCoordinate(x0, width);
        int yStart = getScaledCoordinate(y0, height);
        int xEnd = xStart + grid_scale;
        int yEnd = yStart + grid_scale;
        
        // generate concrete
        for(int y=yStart; y<yEnd; y++)
            for(int x=xStart; x<xEnd; x++)
                canvas.setBlock(x, getScaledAltitude(x, y) - 1, y, Block.STONE);
    }

    protected void renderHighway(int x0, int y0)
    {
        // TODO: not yet implemented

        int xStart = getScaledCoordinate(x0, width);
        int yStart = getScaledCoordinate(y0, height);
        int xEnd = xStart + grid_scale;
        int yEnd = yStart + grid_scale;

        TerrainMap terra = map.getTerrainMap();
        int waterAlt = terra.getWaterAltitude(x0, y0) * grid_scale;
                
        // generate elevated road
        for(int y=yStart; y<yEnd; y++)
            for(int x=xStart; x<xEnd; x++)
                canvas.setBlock(x, Math.max(waterAlt, getScaledAltitude(x, y)) + 16, y, Block.STONE);
    }
    
    protected void renderColumn(int x, int y, int terrainAltitude, int waterAltitude)
    {        
        for(int height=1; height<terrainAltitude; height++)
            canvas.setBlock(x, height, y, getTerrainMaterial(terrainAltitude - height));
        
        for(int height=terrainAltitude; height<waterAltitude; height++)
            canvas.setBlock(x, height, y, Block.WATER);
        
        canvas.setBlock(x, 0, y, Block.BEDROCK);
        canvas.setBiome(x, y, Biome.FOREST);
    }

    protected Block getTerrainMaterial(int depth)
    {
        if(depth <= 1)
            return Block.SANDSTONE;
        else if(depth <= 3)
            return Block.DIRT;
        else
            return Block.STONE;
    }    

    protected int getScaledCoordinate(int p0, int size)
    {
        return p0 * grid_scale - (size >> 1) * grid_scale;
    }
    
    protected int getScaledAltitude(int x, int y)
    {
        float xs = (width >> 1) + (x + 0.5f) / grid_scale;
        float ys = (height >> 1) + (y + 0.5f) / grid_scale;
        return (int)(map.getTerrainMap().getSmoothAltitude(xs, ys) * grid_scale);
    }

    protected int getScaledWaterAltitude(int x, int y)
    {
        int xs = (width >> 1) + x / grid_scale;
        int ys = (height >> 1) + y / grid_scale;
        return map.getTerrainMap().getWaterAltitude(xs, ys) * grid_scale;
    }
    
    protected boolean isBuried(int x, int y, int z)
    {
        return canvas.getBlock(x - 1, y, z).isOpaque()
            && canvas.getBlock(x, y, z - 1).isOpaque()
            && canvas.getBlock(x + 1, y, z).isOpaque()
            && canvas.getBlock(x, y, z + 1).isOpaque();
    }
    
    protected static int getPercentage(int y0)
    {
        return (y0 * 100) / height;
    }
    
    protected static void log(String str)
    {
        System.out.println(str);
    }
}