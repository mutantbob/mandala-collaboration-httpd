package com.purplefrog.mandaladSpring;

import com.purplefrog.mandalad.*;
import org.apache.tika.*;
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

    private MandalaConfig mandala = new MandalaConfig();
    StorageService storageService;

    @Autowired
    public MandalaSpring(StorageService storageService)
    {
        this.storageService = storageService;
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
            int byteCount = storageService.saveRingPart(ring, art);
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
        throws FileNotFoundException
    {
        ResponseEntity x = complainIfBadRing(ring);
        if (x != null) return x;

        if (stripped) {
            PayloadAndMIME result = mandala.getStrippedImage(ring);
            HttpHeaders headers = headersForContentType(result.contentType);

            Object payload;
            if (result.payload instanceof File) {
                File file = (File) result.payload;
                payload = new InputStreamResource(new FileInputStream(file));
                headers.setContentLength(file.length());
            } else {
                payload = result.payload;
            }

            return new ResponseEntity(payload, headers, HttpStatus.OK);
        } else {
            File f = fileForRing(ring);
            if (f.exists()) {
                String mime;
                try {
                    Tika tika = new Tika();
                    mime = tika.detect(f);
                } catch (IOException e) {
                    e.printStackTrace();
                    mime = "application/octet-stream";
                }
                HttpHeaders headers = headersForContentType(mime);
                return new ResponseEntity<>(f, headers, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);
    }

    public static HttpHeaders headersForContentType(String mime)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mime));
        return headers;
    }

    public static File fileForRing(@RequestParam("ring") int ring)
    {
        return MandalaD.fileForRing(ring);
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

        File f = fileForRing(ring);

        boolean success = f.delete();

        mandala.resetPanel(ring);

        String msg;
        if (success) {
            msg = "discarded " + f.getName();
        } else {
            msg = "failed to delete "+f.getName();
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


}
