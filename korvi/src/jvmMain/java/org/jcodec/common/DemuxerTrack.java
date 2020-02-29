package org.jcodec.common;

import org.jcodec.common.model.Packet;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface DemuxerTrack {
    @Nullable
    Packet nextFrame() throws IOException;
    
    DemuxerTrackMeta getMeta();
}
