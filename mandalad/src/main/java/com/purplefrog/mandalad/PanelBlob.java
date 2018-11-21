package com.purplefrog.mandalad;

import java.io.*;

public interface PanelBlob
{
    <T> T convert(BlobConverter<T> converter)
        throws IOException;

    class PanelBlobFile
        implements PanelBlob
    {
        File f;

        public PanelBlobFile(File file)
        {
            f = file;
        }

        @Override
        public <T> T convert(BlobConverter<T> converter)
            throws IOException
        {
            return converter.convertFile(f);
        }
    }


}
