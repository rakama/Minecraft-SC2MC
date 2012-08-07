package com.github.rakama.sc2mc;

import java.util.Random;

import com.github.rakama.sc2mc.map.SC2Map;
import com.github.rakama.sc2mc.map.StructureMap;
import com.github.rakama.sc2mc.map.TerrainMap;
import com.github.rakama.worldtools.canvas.BlockCanvas;
import com.github.rakama.worldtools.data.Biome;
import com.github.rakama.worldtools.data.Block;

/**
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

public class Converter
{
    public static final int width = 128;
    public static final int height = 128;
    public static final int grid_scale = 16;
    
    static Converter instance;
    
    protected Random rand;
    protected BlockCanvas canvas;
    protected SC2Map map;
    
    protected Converter(SC2Map map, BlockCanvas canvas)
    {
        this.map = map;
        this.canvas = canvas;
        this.rand = new Random(0);
    }
    
    public void convert(boolean verbose)
    {
        for(int y=0; y<height; y++)
        {
            if(verbose)
                log("Generating... " + getPercentage(y + 1) + "% complete");
            
            for(int x=0; x<width; x++)
                renderGrid(x, y);
        }
    }
    
    protected void renderGrid(int x0, int y0)
    {
        int xStart = x0 * grid_scale - (width >> 1) * grid_scale;
        int yStart = y0 * grid_scale - (height >> 1) * grid_scale;
        int xEnd = xStart + grid_scale;
        int yEnd = yStart + grid_scale;
                
        TerrainMap terra = map.getTerrainMap();
        int waterAltitude = terra.getWaterAltitude(x0, y0) * grid_scale;
                
        // generate terrain
        for(int y=yStart; y<yEnd; y++)
            for(int x=xStart; x<xEnd; x++)
                renderColumn(x, y, getScaledAltitude(x, y), waterAltitude);
        
        // generate waterfalls
        if(terra.isWaterfall(x0, y0))
            renderWaterfall(x0, y0, terra.getTerrainAltitude(x0, y0));
        
        StructureMap struct = map.getStructureMap();        
        if(struct.isEmptyLot(x0, y0))
        {
            // generate shrub
            if(!terra.isFlooded(x0, y0) && rand.nextDouble() < 0.02)
            {
                int x = xStart + rand.nextInt(grid_scale);
                int y = yStart + rand.nextInt(grid_scale);
                canvas.setBlock(x, getScaledAltitude(x, y), y, Block.SHRUB);
            }
            
            return;
        }
        
        int treeDensity = struct.getTreeDensity(x0, y0);
        int numTrees = (int)Math.floor(treeDensity * 1.5);
        
        // generate trees
        for(int i=0; i<numTrees; i++)
        {
            int x = xStart + rand.nextInt(grid_scale);
            int y = yStart + rand.nextInt(grid_scale);
            int height = 6 + rand.nextInt(4);
            renderTree(x, y, getScaledAltitude(x, y), height);
        }
    }

    protected int getScaledAltitude(int x, int y)
    {
        float xs = (width >> 1) + x / (float)grid_scale;
        float ys = (height >> 1) + y / (float)grid_scale;
        return (int)(map.getTerrainMap().getSmoothAltitude(xs, ys) * grid_scale);
    }
    
    protected void renderWaterfall(int x0, int y0, int altitude)
    {
        int xStart = x0 * grid_scale - (width >> 1) * grid_scale;
        int yStart = y0 * grid_scale - (height >> 1) * grid_scale;
        int xEnd = xStart + grid_scale;
        int yEnd = yStart + grid_scale;
        int altStart = altitude * grid_scale - 1;
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

    protected void renderTree(int x, int y, int altitude, int height)
    {        
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