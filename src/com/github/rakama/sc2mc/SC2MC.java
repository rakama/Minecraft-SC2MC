package com.github.rakama.sc2mc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.github.rakama.sc2mc.map.AltitudeMap;
import com.github.rakama.sc2mc.map.SC2Map;
import com.github.rakama.worldtools.WorldTools;
import com.github.rakama.worldtools.canvas.BlockCanvas;
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
    static int grid_scale = 16;
    
    static String input = "C:/Program Files (x86)/GOG.com/SimCity 2000 Special Edition/CUSTOM/TEST.SC2";
    static String output = "C:/Users/My Computer/AppData/Roaming/.minecraft/saves/sc2test";
    
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
        AltitudeMap alt = map.getAltitudeMap();
        
        for(int x=0; x<128; x++)
            for(int y=0; y<128; y++)
                renderGrid(canvas, x, y, alt.getAltitude(x, y), alt.isWater(x, y));
    }
    
    public static void renderGrid(BlockCanvas canvas, int x0, int y0, int altitude, boolean water)
    {
        int xStart = x0 * grid_scale - 64 * grid_scale;
        int yStart = y0 * grid_scale - 64 * grid_scale;
        int xEnd = xStart + grid_scale;
        int yEnd = yStart + grid_scale;
        
        altitude *= grid_scale;
        
        System.out.println("Reticulating (" + x0 + ", " + y0 + ") ...");
        
        for(int x=xStart; x<xEnd; x++)
            for(int y=yStart; y<yEnd; y++)
                renderColumn(canvas, x, y, altitude, water);
    }
    
    public static void renderColumn(BlockCanvas canvas, int x, int y, int altitude, boolean water)
    {
        Block block;
        
        if(water)
            block = Block.WATER;
        else
            block = Block.SANDSTONE;
        
        for(int height=1; height<altitude; height++)
            canvas.setBlock(x, height, y, block);
        
        canvas.setBlock(x, 0, y, Block.BEDROCK);
    }
}