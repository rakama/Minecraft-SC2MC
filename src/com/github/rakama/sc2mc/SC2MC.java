package com.github.rakama.sc2mc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

import com.github.rakama.sc2mc.map.AltitudeMap;
import com.github.rakama.sc2mc.map.SC2Map;
import com.github.rakama.sc2mc.map.StructureMap;
import com.github.rakama.worldtools.WorldTools;
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

public class SC2MC
{
    static String input = "C:/Program Files (x86)/GOG.com/SimCity 2000 Special Edition/CUSTOM/TESTMAP.SC2";
    static String output = "C:/Users/My Computer/AppData/Roaming/.minecraft/saves/sc2test";
    
    static final int width = 128;
    static final int height = 128;
    static final int grid_scale = 16;
    
    static Random rand = new Random(0);
    
    public static void main(String[] args) throws IOException
    {
        SC2Map map = SC2Map.loadMap(new FileInputStream(new File(input)));
        WorldTools tools = WorldTools.getInstance(new File(output));
        BlockCanvas canvas = tools.createCanvas();
        renderHeightmap(canvas, map);
        tools.closeAll();
    }
    
    public static void renderHeightmap(BlockCanvas canvas, SC2Map map)
    {
        for(int y=0; y<height; y++)
        {
            System.out.println("Reticulating mines... " + getPercentage(y + 1) + "%");            
            for(int x=0; x<width; x++)
                renderGrid(canvas, map, x, y);
        }

        System.out.println("Finished!");  
    }
    
    public static void renderGrid(BlockCanvas canvas, SC2Map map, int x0, int y0)
    {
        int xStart = x0 * grid_scale - (width >> 1) * grid_scale;
        int yStart = y0 * grid_scale - (height >> 1) * grid_scale;
        int xEnd = xStart + grid_scale;
        int yEnd = yStart + grid_scale;
                
        AltitudeMap alt = map.getAltitudeMap();
        int waterAltitude = alt.getWaterAltitude(x0, y0) * grid_scale;
                
        // generate terrain
        for(int y=yStart; y<yEnd; y++)
            for(int x=xStart; x<xEnd; x++)
                renderColumn(canvas, x, y, getScaledAltitude(alt, x, y), waterAltitude);
        
        StructureMap struct = map.getStructureMap();
        
        if(struct.isEmptyLot(x0, y0))
            return;
        
        int treeDensity = struct.getTreeDensity(x0, y0);
        int numTrees = (int)Math.floor(treeDensity * 1.5);
        
        // generate trees
        for(int i=0; i<numTrees; i++)
        {
            int x = xStart + rand.nextInt(grid_scale);
            int y = yStart + rand.nextInt(grid_scale);
            int height = 6 + rand.nextInt(4);
            renderTree(canvas, x, y, getScaledAltitude(alt, x, y), height);
        }
    }
    
    public static void renderTree(BlockCanvas canvas, int x, int y, int altitude, int height)
    {        
        for(int i=2; i<height; i++)
            for(int j=0; j<3; j++)
                for(int k=0; k<3; k++)
                    if((i & 1) == 0 || (j & 1) != (k & 1))
                        canvas.setBlock(x + j - 1, altitude + i, y + k - 1, Block.LEAVES);

        canvas.setBlock(x, altitude + height, y, Block.LEAVES);
        
        for(int i=0; i<height; i++)
            canvas.setBlock(x, altitude + i, y, Block.WOOD);
    }
    
    public static void renderColumn(BlockCanvas canvas, int x, int y, int terrain, int water)
    {        
        for(int height=1; height<terrain; height++)
            canvas.setBlock(x, height, y, Block.SANDSTONE);
        
        for(int height=terrain; height<water; height++)
            canvas.setBlock(x, height, y, Block.WATER);
        
        canvas.setBlock(x, 0, y, Block.BEDROCK);
        canvas.setBiome(x, y, Biome.FOREST);
    }
    
    private static int getScaledAltitude(AltitudeMap alt, int x, int y)
    {
        float xs = (width >> 1) + x / (float)grid_scale;
        float ys = (height >> 1) + y / (float)grid_scale;
        return (int)(alt.getSmoothAltitude(xs, ys) * grid_scale);
    }
    
    private static int getPercentage(int y0)
    {
        return (y0 * 100) / height;
    }
}