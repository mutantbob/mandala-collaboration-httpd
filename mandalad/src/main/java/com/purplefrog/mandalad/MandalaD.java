package com.purplefrog.mandalad;


import com.purplefrog.apachehttpcliches.*;
import com.purplefrog.httpcliches.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.protocol.*;
import org.apache.log4j.*;
import org.stringtemplate.v4.*;

import javax.jws.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MandalaD
    implements HttpRequestHandler
{
    private static final Logger logger = Logger.getLogger(MandalaD.class);

    public final String prefix;

    MandalaConfig mandala;


    public MandalaD(String prefix, StorageService storageService)
    {
        this.prefix = prefix;
        mandala = new MandalaConfig(storageService);
    }

    public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext)
    {
        EntityAndHeaders result;

        try {
            URI uri = new URI(httpRequest.getRequestLine().getUri());
            if (!uri.getPath().startsWith(prefix)) {
                result = EntityAndHeaders.plainTextPayload(404, "not found");
            } else {
                String suffix = uri.getPath().substring(prefix.length());

                result = handle_(httpRequest, httpContext, suffix);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("malfunction processing web request", e);
            result = EntityAndHeaders.plainTextPayload(500, "I am full of explosions:\n"+ Util2.stringStackTrace(e));
        }

        result.apply(httpResponse);

    }

    public EntityAndHeaders handle_(HttpRequest req, HttpContext ctx, String suffix)
        throws IOException, FileUploadException, URISyntaxException, CGIWebMethod.CGISOAPTransformException, InvocationTargetException, IllegalAccessException
    {
        if ("".equals(suffix)) {
            return new EntityAndHeaders.Redirect(ApacheHTTPCliches.redirectPath(ctx, prefix+"/"), "moved");
        }
        if ("/".equals(suffix)) {
            return rootPage();
        }

        Method m = CGIWebMethod.matchName(getClass(), suffix.substring(1));
        if (null != m) {
            CGIEnvironment cgiEnv = ApacheCGI.parseEnv(req, ctx);
            Object rval = m.invoke(this, CGIWebMethod.transformCGIArgumentsToJavaParams(m, cgiEnv));
            if (rval instanceof EntityAndHeaders)
                return (EntityAndHeaders) rval;
            else
                return EntityAndHeaders.plainTextPayload(200, rval.toString());
        }

        if ("/uploadTest".equals(suffix)) {
            FileUploadBase upload = new FileUpload(new DiskFileItemFactory(1<<20, new File("/tmp/mandalad/")));
            List<FileItem> parts = upload.parseRequest(new ApacheMultipartContext(req));
            StringBuilder payload = new StringBuilder(upload.toString() + "\n" + parts + "\n\n");
            for (FileItem part : parts) {
                payload.append(part.getFieldName() + " " + part.get().length+" "+part.getContentType()+"\n\n");
            }
            return EntityAndHeaders.plainTextPayload(200, payload.toString());
        }

        return EntityAndHeaders.plainTextPayload(404, "not found");
    }

    public EntityAndHeaders rootPage()
        throws IOException
    {
        ST st = rootPage2(mandala, getClass());

        return EntityAndHeaders.plainPayload(200, st.render(), "text/html");
    }

    public static ST rootPage2(MandalaConfig mandala, Class cls)
        throws IOException
    {
        InputStream istr = cls.getResourceAsStream("index.html");
        String template = Util2.slurp(new InputStreamReader(istr));

        ST st = new ST(HTMLEnabledObject.makeSTGroup(true, '$', '$'), template);

        List<Map> rings = new ArrayList<>();
        for (int i = 0; i< mandala.ringCount(); i++) {
            TreeMap map = new TreeMap();
            map.put("index", i);
            map.put("imageURL", "image?stripped=true&ring="+i);
            rings.add(map);
        }
        st.add("rings", rings);
        return st;
    }

    @WebMethod
    public EntityAndHeaders upload2(@WebParam(name = "ring") int ring,
                                    @WebParam(name="art") FileItem art,
                                    @WebParam CGIEnvironment env)
        throws IOException
    {
        System.out.println("upload from "+env.remoteAddressString()+" to ring "+ring+" of "+art.getSize()+" bytes");

        EntityAndHeaders x = complainIfBadRing(ring);
        if (x != null) return x;

        int sizeLimit = 10 << 20;
        if (art.getSize()>sizeLimit)
            return EntityAndHeaders.plainTextPayload(403, "file too large, upload cancelled");

        long count = mandala.uploadPanelArt(ring, art.getSize(), art.getInputStream());

        return EntityAndHeaders.plainTextPayload(200, "wrote "+count+" bytes to "+ring);
    }

    public EntityAndHeaders complainIfBadRing(@WebParam(name = "ring") int ring)
    {
        if (ring >= mandala.ringCount())
            return EntityAndHeaders.plainTextPayload(403, "bad ring number "+ring+">="+mandala.ringCount());
        if (ring < 0)
            return EntityAndHeaders.plainTextPayload(403, "bad ring number "+ring+"<0");
        return null;
    }

    @WebMethod
    public EntityAndHeaders deletePanel(@WebParam(name = "ring") int ring)
    {
        EntityAndHeaders x = complainIfBadRing(ring);
        if (x != null) return x;

        boolean success = mandala.deletePanel(ring);

        String msg;
        if (success) {
            msg = "discarded " + ring;
        } else {
            msg = "failed to delete "+ring;
        }
        return EntityAndHeaders.plainTextPayload(200, msg);
    }

    @WebMethod
    public EntityAndHeaders rings()
    {
        String payload = mandala.asSVG();
        return EntityAndHeaders.plainPayload(200, payload, "image/svg+xml");
    }

    @WebMethod
    public EntityAndHeaders image(@WebParam(name="ring") int ring,
                                  @WebParam(name="stripped") boolean stripped)
        throws IOException
    {
        EntityAndHeaders x = complainIfBadRing(ring);
        if (x != null) return x;

        PayloadAndMIME response;
        if (stripped) {
            response = mandala.getStrippedImage(ring);
        } else {
            response = mandala.getImage(ring);
        }

        if (response != null)
            return toEntity(response);

        return EntityAndHeaders.plainTextPayload(404, "not found");
    }

    public static EntityAndHeaders toEntity(PayloadAndMIME response)
        throws IOException
    {
        final ContentType contentType = ContentType.create(response.contentType);
        return response.payload.convert(new BlobConverter<EntityAndHeaders>()
        {
            @Override
            public EntityAndHeaders convertFile(File f)
                throws IOException
            {
                return new EntityAndHeaders(200, new FileEntity(f, contentType));
            }

            @Override
            public EntityAndHeaders convertString(String message)
            {
                return EntityAndHeaders.plainPayload(200, message, response.contentType);
            }
        });

       }

    @WebMethod
    public EntityAndHeaders template(@WebParam(name="ring") int ring)
    {
        EntityAndHeaders x = complainIfBadRing(ring);
        if (x != null) return x;

        String payload = mandala.templateForRing(ring);

        return EntityAndHeaders.plainPayload(200, payload, "image/svg+xml");
    }

    public static void main(String[] argv)
        throws IOException
    {
        ExecutorService executor = Executors.newCachedThreadPool();
        HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
        int port = 4044;
        BasicHTTPAcceptLoop loop = new BasicHTTPAcceptLoop(port, registry, executor);

        StorageService storageService= new FileStorageService();
        MandalaD md = new MandalaD("/mandala", storageService);
        registry.register(md.prefix+"*", md);

        System.out.println("accepting connections at http://"+ loop.getAddress());
        loop.run();
    }

}
