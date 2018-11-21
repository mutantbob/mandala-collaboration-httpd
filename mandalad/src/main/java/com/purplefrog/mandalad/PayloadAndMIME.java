package com.purplefrog.mandalad;

import java.io.*;

public class PayloadAndMIME
{
    public final PanelBlob payload;
    public final String contentType;

    public PayloadAndMIME(PanelBlob payload, String contentType)
    {
        this.payload = payload;
        this.contentType = contentType;
    }

    public PayloadAndMIME(String msg, String contentType)
    {
        this.payload = new PanelBlob()
        {
            @Override
            public <T> T convert(BlobConverter<T> converter)
                throws IOException
            {
                return converter.convertString(msg);
            }
        };
        this.contentType = contentType;
    }
}
