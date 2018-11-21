package com.purplefrog.mandaladSpring;

import com.purplefrog.mandalad.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import java.io.*;

@Controller
public class MandalaSpring
{

    private MandalaConfig mandala;
    StorageService storageService;

    @Autowired
    public MandalaSpring(StorageService storageService)
    {
        this.storageService = storageService;
        mandala = new MandalaConfig(this.storageService);
    }

    @GetMapping("/")
    @ResponseBody
    public String rootPage()
        throws IOException
    {
        return MandalaD.rootPage2(mandala, MandalaD.class).render();
    }


    @RequestMapping(method = RequestMethod.POST, value="/upload2")
    public ResponseEntity<String> upload2(
        @RequestParam(name = "ring") int ring,
        @RequestParam(name="art") MultipartFile art
        )
    {
        try {
            int byteCount = storageService.saveRingPart(ring, art.getSize(), art.getInputStream());
            mandala.resetPanel(ring);
            return new ResponseEntity<>("Groovy [" +byteCount+ "]", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getLocalizedMessage(), HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value="/image")
    public ResponseEntity image(@RequestParam("stripped") boolean stripped,
                                @RequestParam("ring") int ring)
        throws IOException
    {
        ResponseEntity x = complainIfBadRing(ring);
        if (x != null) return x;

        PayloadAndMIME result;
        if (stripped) {
            result = mandala.getStrippedImage(ring);
        } else {
            result = mandala.getImage(ring);
        }


        return result.payload.convert(new ResponseEntityBlobConverter(result.contentType));
    }

    public static HttpHeaders headersForContentType(String mime)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mime));
        return headers;
    }

    @RequestMapping(method = RequestMethod.GET, value="/rings")
    public ResponseEntity<String> rings()
    {
        String payload = mandala.asSVG();
        HttpHeaders headers = headersForContentType("image/svg+xml");
        return new ResponseEntity<>(payload, headers, HttpStatus.OK);
    }

    @RequestMapping("/deletePanel")
    public ResponseEntity<String> deletePanel(@RequestParam("ring") int ring)
    {
        ResponseEntity<String> x = complainIfBadRing(ring);
        if (x != null) return x;

        boolean success = storageService.deleteRingPart(ring);

        mandala.resetPanel(ring);

        String msg;
        if (success) {
            msg = "discarded " + ring;
        } else {
            msg = "failed to delete "+ring;
        }
        return new ResponseEntity<>(msg, HttpStatus.OK);
    }

    @RequestMapping("/template")
    public ResponseEntity<String> template(@RequestParam(name="ring") int ring)
    {
        ResponseEntity<String> x = complainIfBadRing(ring);
        if (x != null) return x;

        String payload = mandala.templateForRing(ring);

        HttpHeaders headers = headersForContentType("image/svg+xml");
        return new ResponseEntity<>(payload, headers, HttpStatus.OK);

    }

    public ResponseEntity<String> complainIfBadRing(int ring)
    {

        if (ring >= mandala.ringCount())
            return new ResponseEntity<>( "bad ring number "+ring+">="+mandala.ringCount(), HttpStatus.FORBIDDEN);
        if (ring < 0)
            return new ResponseEntity<>( "bad ring number "+ring+"<0", HttpStatus.FORBIDDEN);
        return null;
    }

    public static class ResponseEntityBlobConverter
        implements BlobConverter<ResponseEntity>
    {
        HttpHeaders headers;

        public ResponseEntityBlobConverter(String contentType)
        {
            headers = headersForContentType(contentType);
        }

        @Override
        public ResponseEntity convertFile(File file)
            throws IOException
        {
            headers.setContentLength(file.length());
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        }

        @Override
        public ResponseEntity convertString(String s)
            throws IOException
        {
            return new ResponseEntity<>(s, headers, HttpStatus.OK);
        }
    }
}
