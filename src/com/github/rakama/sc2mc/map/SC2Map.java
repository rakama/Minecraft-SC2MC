package com.github.rakama.sc2mc.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.rakama.sc2mc.io.SegInputStream;
import com.github.rakama.sc2mc.io.Segment;

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

public class SC2Map
{
    protected Map<String, Segment> segments;
    protected int fileSize;
    
    protected TerrainMap terrainMap;
    protected StructureMap structureMap;
    
    protected SC2Map(int fileSize)
    {
        this.segments = new HashMap<String, Segment>();
        this.fileSize = fileSize;
    }
    
    public static SC2Map loadMap(InputStream in) throws IOException
    {
        SegInputStream segin = new SegInputStream(in);
        
        // confirm that this file is IFF format
        checkHeader(segin, "FORM");
        
        long fileSize = segin.readUInt32();   
        if(fileSize < 0 || fileSize > Integer.MAX_VALUE)
            throw new IOException("Invalid file size!");
                
        // confirm this file is a SimCity 2000 map
        checkHeader(segin, "SCDH");
        
        SC2Map map = new SC2Map((int)fileSize + 4);
        
        // read segments
        Segment seg = segin.readSegment();       
        while(seg != null)
        {
            map.addSegment(seg);
            seg = segin.readSegment(); 
        }

        Segment altm = map.getSegment("ALTM");       
        if(altm == null)
            throw new IOException("ALTM segment not found!");   
        
        Segment xter = map.getSegment("XTER");
        if(xter == null)
            throw new IOException("XTER segment not found!");
        
        xter.decompressData();

        Segment xbld = map.getSegment("XBLD");
        if(xbld == null)
            throw new IOException("XBLD segment not found!");

        xbld.decompressData();

        map.terrainMap = new TerrainMap(altm.getRawData(), xter.getDecompressedData());
        map.structureMap = new StructureMap(xbld.getDecompressedData());
        
        return map;
    }

    private static String checkHeader(SegInputStream in, String header) throws IOException
    {
        String str = in.readString(4);

        if(str == null)
            throw new IOException("File ended prematurely!");
        
        if(!str.equals(header))
            throw new IOException("Invalid header '" + str + "' (expected '" + header + "')");
    
        return str;
    }
    
    private void addSegment(Segment segment)
    {
        segments.put(segment.getName(), segment);
    }
    
    public Segment getSegment(String chunk)
    {
        return segments.get(chunk);
    }

    public Collection<Segment> getSegments()
    {
        return Collections.unmodifiableCollection(segments.values());
    }
    
    public TerrainMap getTerrainMap()
    {
        return terrainMap;
    }
    
    public StructureMap getStructureMap()
    {
        return structureMap;
    }    
    
    public int getFileSize()
    {
        return fileSize;
    }
}