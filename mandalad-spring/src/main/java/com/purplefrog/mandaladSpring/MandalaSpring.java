package com.purplefrog.mandaladSpring;

import com.purplefrog.mandalad.*;
import org.apache.tika.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;

@SpringBootApplication
@Controller
public class MandalaSpring
{

    private MandalaConfig mandala = new MandalaConfig();

    @GetMapping("/")
    @ResponseBody
    public String rootPage()
        throws IOException
    {
        return MandalaD.rootPage2(mandala, MandalaD.class).render();
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(result.contentType) );

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
            File f = MandalaD.fileForRing(ring);
            if (f.exists()) {
                String mime;
                try {
                    Tika tika = new Tika();
                    mime = tika.detect(f);
                } catch (IOException e) {
                    e.printStackTrace();
                    mime = "application/octet-stream";
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(new MediaType(mime));
                return new ResponseEntity<>(f, headers, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>("not found", HttpStatus.NOT_FOUND);

    }

    @RequestMapping(method = RequestMethod.GET, value="/rings")
    public ResponseEntity<String> rings()
    {
        String payload = mandala.asSVG();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/svg+xml"));
        return new ResponseEntity<>(payload, headers, HttpStatus.OK);
    }

    private ResponseEntity complainIfBadRing(int ring)
    {

        if (ring >= mandala.ringCount())
            return new ResponseEntity<>( "bad ring number "+ring+">="+mandala.ringCount(), HttpStatus.FORBIDDEN);
        if (ring < 0)
            return new ResponseEntity<>( "bad ring number "+ring+"<0", HttpStatus.FORBIDDEN);
        return null;
    }

    public static void main(String[] argv)
    {
        SpringApplication.run(MandalaSpring.class);
    }
}
