package com.purplefrog.mandalad;

import com.purplefrog.httpcliches.*;
import org.apache.log4j.*;
import org.apache.tika.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.*;
import org.jdom.output.*;

import java.io.*;
import java.util.*;

public class MandalaConfig
{
    private static final Logger logger = Logger.getLogger(MandalaConfig.class);

    List<Ring> rings;

    public MandalaConfig()
        throws IOException
    {
        rings = new ArrayList<Ring>();

        int oldCount=0;

        int nRings = 8;
        int h = 60;

        for (int ring=0; ring<nRings; ring++)
        {
            int radius = 40+(ring)*50;
            double phase ;
            if (ring%2==0)
                phase = 0;
            else
                phase = 0.5;

            double w;
            if (ring<3) {
                w = 60;
            } else {
                w = 80;
            }

            int minCount = (int) Math.ceil( radius*2*Math.PI / w );
            int count = nextCount(oldCount, minCount);
            rings.add(new Ring(phase, count, new PlaceholderPanel(ring), w, h, radius));
            System.out.println("ring="+ring+" ; count="+count);

            oldCount = count;
        }
    }

    public void resetPanel(int ring)
    {
        rings.get(ring).resetPanel();
    }

    public int ringCount()
    {
        return rings.size();
    }

    public static int nextCount(int oldCount, int minCount)
    {
        if (oldCount >= minCount)
            return oldCount;

        if (oldCount%2==0) {
            int count = 3 * oldCount / 2;
            if (count >= minCount)
                return count;
        }

        int count = 2*oldCount;
        if (count>= minCount)
            return count;

        return minCount;
    }

    public String asSVG()
    {
        StringBuilder rval = new StringBuilder();
        int svgWidth = 1000;
        int svgHeight = 1000;
        rval.append("<svg width=\"" + svgWidth + "\" height=\"" + svgHeight + "\"" +
            " viewbox=\" 0 0 " +svgWidth+" "+svgHeight+ "\" xmlns=\"http://www.w3.org/2000/svg\"" +
            " xmlns:xlink=\"http://www.w3.org/1999/xlink\" >\n");
        rval.append("<g transform=\"translate(" +svgWidth/2+ "," +svgHeight/2+ ")\">\n");
        for (int i=rings.size()-1; i>=0; i--) {
            rval.append( svgForRing(rings.get(i), i) );
        }
        rval.append("</g>\n");
        rval.append("</svg>\n");
        return rval.toString();
    }

    public String svgForRing(Ring ring1, int ring)
    {
        StringBuilder rval = new StringBuilder();
        String id = "ring"+ring;
        if (ring==0) {
            rval.append(ring1.mPanel.toSVG(ring1.width, ring1.height, 0.5, 0.5));
        } else {

            for (int j = 0; j <= ring1.count; j++) {
                double degrees = (j + ring1.phase) / ring1.count * 360;
                double dy = -(ring1.radius + 0.5 * ring1.height);
                rval.append("<g transform=\"rotate(" + degrees + ") translate(" + 0 + "," + dy + ")\">\n");
                if (j == 0) {
                    rval.append("<g id=\"" + id + "\">\n");
                    rval.append(ring1.mPanel.toSVG(ring1.width, ring1.height, 0.5, 0));
                    rval.append("\n</g>\n");
                } else if (j == ring1.count) {
                    rval.append("<use xlink:href=\"#" + id + "\" clip-path=\"url(#clip" + ring + ")\"/>\n");
                    int yPad = 50;
                    rval.append("<clipPath id=\"clip" + ring + "\">\n");
                    rval.append("<rect x=\"" + (
                        -ring1.width//"0"
                    ) + "\" y=\"" + (-yPad) + "\" width=\"" + ring1.width + "\" height=\"" + (ring1.height + 2 * yPad) + "\"" +
                        " style=\"stroke:#f00; fill:none\"/>");
                    rval.append("</clipPath>\n");
                } else {
                    rval.append("<use xlink:href=\"#" + id + "\"/>\n");
                }
                rval.append("</g>\n");
            }
        }
        return rval.toString();
    }

    public static class Ring
    {
        double phase;
        int count;
        double radius;
        MandalaPanel mPanel;
        double width;
        double height;

        public Ring(double phase, int count, MandalaPanel mPanel,
                    double width, double height, double radius)
        {
            this.phase = phase;
            this.count = count;
            this.mPanel = mPanel;
            this.width = width;
            this.height = height;
            this.radius = radius;
        }

        public void resetPanel()
        {
            mPanel.resetPanel();
        }
    }

    public interface MandalaPanel
    {
        String toSVG(double width, double height, double hAlign, double vAlign);

        void resetPanel();
    }

    public static class PNGMandalaPanel
        implements MandalaPanel
    {
        private final String blob;
        public PNGMandalaPanel(File imageFile, String mime)
            throws IOException
        {
            InputStream istr = new FileInputStream(imageFile);
            StringBuilder base64 = fileToBase64(istr);
            blob = "data:"+mime+";base64,"+base64;
        }

        public String toSVG(double width, double height, double hAlign, double vAlign)
        {
            double x = -hAlign *width;
            double y = -vAlign *height;
            return blobImage(x, y, width, height, blob);
        }

        @Override
        public void resetPanel()
        {

        }
    }

    public static String blobImage(double x, double y, double width, double height, String blob)
    {
        return "<image width=\""+width+"\" height=\"" +height+
            "\" x=\"" + x + "\" y=\"" + y + "\"" +
            " xlink:href=\"" +blob+ "\"/>";
    }

    public static class PlaceholderPanel
    implements MandalaPanel
    {
        private final int ring;
        String message;
        File imageFile;
        MandalaPanel delegate;

        public PlaceholderPanel(int ring)
        {
            this.ring = ring;
            this.message = ""+ring;//(char)(0x41+ring);
            imageFile = MandalaD.fileForRing(ring);
        }

        @Override
        public String toSVG(double width, double height, double hAlign, double vAlign)
        {
            if (delegate == null ) {
                if (imageFile.exists()) {
                    try {
                        Tika tika = new Tika();
                        String mime = tika.detect(imageFile);
                        if ("image/svg+xml".equals(mime)) {
                            String payload = extractArtFromSVG(imageFile);
                            delegate = new MandalaPanel()
                            {
                                @Override
                                public String toSVG(double width, double height, double hAlign, double vAlign)
                                {
                                    return payload;
                                }

                                @Override
                                public void resetPanel() { }
                            };
                        } else {
                            try {
                                delegate = new PNGMandalaPanel(imageFile, mime);
                            } catch (IOException e) {
                                logger.warn("failed to load "+ imageFile);
                            }
                        }
                    } catch (IOException e) {
                        logger.warn("", e);
                    }
                }
            }

            if (delegate !=null) {
                return delegate.toSVG(width, height, hAlign, vAlign);
            }

            return placeholder(width, height, 0.5, 0.0);
        }

        public String extractArtFromSVG(File imageFile)
            throws IOException
        {
            try {
                SAXBuilder builder  = new SAXBuilder();
                Document doc = builder.build(imageFile);
                Element root = doc.getRootElement();

                double width = Double.parseDouble(root.getAttributeValue("width"));
                double height = Double.parseDouble(root.getAttributeValue("height"));

                List<Element> children = root.getChildren();

                XMLOutputter xout = new XMLOutputter();
                StringBuilder rval = new StringBuilder();

                rval.append("<g transform=\"translate(" +
                    -0.5*width+
                    "," +
                    0+
                    ")\">\n");

                for (Element child : children) {
                    if ("defs".equals(child.getName()))
                        continue;
                    if ("namedview".equals(child.getName()))
                        continue;
                    if ("metadata".equals(child.getName()))
                        continue;
                    rval.append(xout.outputString(child));
                }
                rval.append("</g>\n");

                return rval.toString();
            } catch (Exception e) {
                logger.warn("failed to parse "+imageFile, e);

                return Util2.slurp(new FileReader(imageFile));
            }
        }

        public String placeholder(double width, double height, double hAlign, double vAlign)
        {
            StringBuilder rval = new StringBuilder();
            {
                double x = -hAlign * width;
                double y = -vAlign *height;
                rval.append("<rect x=\"" + x + "\" y=\"" + y + "\"" +
                    " width=\"" +width+ "\" height=\"" +height+ "\"" +
                    " style=\"fill:#ffc; stroke:#000\"/>\n");
            }
            int fontSize = 20;
            rval.append("<text x=\"0\" y=\"" +((0.5-vAlign)*height+0.5* fontSize)+
                "\" style=\"fill:#000; stroke:none; font-size:" + fontSize + "px; text-align:middle\">" +
                message+"</text>");
            return rval.toString();
        }

        @Override
        public void resetPanel()
        {
            delegate = null;
        }
    }

    public static StringBuilder fileToBase64(InputStream istr)
        throws IOException
    {
        StringBuilder base64 = new StringBuilder();

        Base64.Encoder enc = Base64.getEncoder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream ostr = enc.wrap(baos);
        byte[] buffer = new byte[64<<10];
        while (true) {
            int n = istr.read(buffer);
            if (n<1)
                break;
            ostr.write(buffer, 0, n);
        }
        ostr.flush();

        base64.append(new String(baos.toByteArray()));
        return base64;
    }
}
