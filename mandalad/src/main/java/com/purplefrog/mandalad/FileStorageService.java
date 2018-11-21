package com.purplefrog.mandalad;

import java.io.*;

public class FileStorageService
    implements StorageService
{
    @Override
    public int saveRingPart(int ring, long byteCount, InputStream artStream)
        throws IOException
    {
        int sizeLimit = 10<<20;
        if (byteCount > sizeLimit) {
            throw new IllegalArgumentException("file too large, upload cancelled");
        }

        File f = fileForRing(ring);

        File tmpFile = new File(f.getPath()+".new");


        byte[] buffer = new byte[64<<10];
        OutputStream ostr = new FileOutputStream(tmpFile);
        int count=0;
        while (true) {
            int n = artStream.read(buffer);
            if (n<1)
                break;
            ostr.write(buffer, 0, n);
            count += n;

            if (count > sizeLimit) {
                tmpFile.delete();
                throw new IllegalArgumentException("file too large, upload cancelled");
            }
        }
        ostr.close();
        artStream.close();

        f.delete();
        tmpFile.renameTo(f);

        return count;
    }

    public static File fileForRing(int ring)
    {
        File dir = new File("/var/tmp/mandala/");
        String basename = "panel" + ring ;
        return new File(dir, basename);
    }


    @Override
    public PanelBlob getRingPart(int ring)
    {
        return new PanelBlob.PanelBlobFile(fileForRing(ring));
    }

    @Override
    public boolean deleteRingPart(int ring)
    {
        File f = fileForRing(ring);
        return f.delete();
    }
}
