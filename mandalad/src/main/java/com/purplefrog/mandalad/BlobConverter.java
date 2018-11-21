package com.purplefrog.mandalad;

import java.io.*;

public interface BlobConverter <T>
{
    T convertFile(File f)
        throws IOException;

    T convertString(String message)
        throws IOException;
}
