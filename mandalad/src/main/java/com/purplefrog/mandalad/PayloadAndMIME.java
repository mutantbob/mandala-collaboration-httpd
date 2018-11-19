package com.purplefrog.mandalad;

public class PayloadAndMIME
{
    public final Object payload;
    public final String contentType;

    public PayloadAndMIME(Object payload, String contentType)
    {
        this.payload = payload;
        this.contentType = contentType;
    }
}
