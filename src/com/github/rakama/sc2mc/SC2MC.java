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

package com.github.rakama.sc2mc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.github.rakama.sc2mc.map.SC2Map;
import com.github.rakama.worldtools.WorldTools;
import com.github.rakama.worldtools.canvas.BlockCanvas;

public class SC2MC
{
    static String input = "C:/Program Files (x86)/GOG.com/SimCity 2000 Special Edition/CITIES/CAPEQUES.SC2";
    static String output = "C:/Users/My Computer/AppData/Roaming/.minecraft/saves/sc2test";

    public static void main(String[] args) throws IOException
    {
        SC2Map map = SC2Map.loadMap(new FileInputStream(new File(input)));
        WorldTools tools = WorldTools.getInstance(new File(output));
                
        System.out.println("Reticulating mines...");

        BlockCanvas canvas = tools.createCanvas();
        Converter converter = new Converter(map, canvas);
        converter.convert(true);

        System.out.println("Saving open chunks ...");
        
        tools.closeAll();
        
        System.out.println("Finished!");  
    }
}