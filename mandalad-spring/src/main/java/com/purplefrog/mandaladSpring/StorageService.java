package com.purplefrog.mandaladSpring;

import com.purplefrog.mandalad.*;
import org.springframework.stereotype.*;
import org.springframework.web.multipart.*;

import java.io.*;

@Service
public class StorageService
{
    public int saveRingPart(int ring, MultipartFile art)
        throws IOException
    {
        int sizeLimit = 10<<20;
        if (art.getSize() > sizeLimit) {
            throw new IllegalArgumentException("file too large, upload cancelled");
        }

        File f = MandalaD.fileForRing(ring);

        File tmpFile = new File(f.getPath()+".new");


        byte[] buffer = new byte[64<<10];
        InputStream istr = art.getInputStream();
        OutputStream ostr = new FileOutputStream(tmpFile);
        int count=0;
        while (true) {
            int n = istr.read(buffer);
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
        istr.close();

        f.delete();
        tmpFile.renameTo(f);

        return count;
    }
}
