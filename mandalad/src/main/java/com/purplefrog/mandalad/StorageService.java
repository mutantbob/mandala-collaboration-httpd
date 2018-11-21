package com.purplefrog.mandalad;

import java.io.*;

public interface StorageService
{
    int saveRingPart(int ring, long byteCount, InputStream artStream)
        throws IOException;

    PanelBlob getRingPart(int ring);

    boolean deleteRingPart(int ring);
}
