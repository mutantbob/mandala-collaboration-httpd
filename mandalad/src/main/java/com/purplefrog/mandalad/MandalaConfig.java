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

    public static double yOffsetForRing(Ring ring0)
    {
        return -(ring0.radius + 0.5*ring0.height);
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
        rval.append(svgHeader(svgWidth, svgHeight));
        rval.append("<g transform=\"translate(" +svgWidth/2+ "," +svgHeight/2+ ")\">\n");
        for (int i=rings.size()-1; i>=0; i--) {
            rval.append( svgForRing(rings.get(i), i) );
        }
        rval.append("</g>\n");
        rval.append("</svg>\n");
        return rval.toString();
    }

    public static String svgHeader(double svgWidth, double svgHeight)
    {
        return "<svg width=\"" + svgWidth + "\" height=\"" + svgHeight + "\"" +
            " viewbox=\" 0 0 " +svgWidth+" "+svgHeight+ "\" xmlns=\"http://www.w3.org/2000/svg\"" +
            " xmlns:xlink=\"http://www.w3.org/1999/xlink\" >\n";
    }

    public String svgForRing(Ring ring1, int ring)
    {
        StringBuilder rval = new StringBuilder();
        String id = "ring"+ring;
        if (ring==0) {
            rval.append(ring1.mPanel.toSVG(ring1.width, ring1.height, 0.5, 0.5));
        } else {

            for (int j = 0; j <= ring1.count; j++) {
                double degrees = degreesForPanel(ring1, j);
                double dy = yOffsetForRing(ring1);
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

    public static double degreesForPanel(Ring ring, int instanceIndex)
    {
        return (instanceIndex + ring.phase) / ring.count * 360;
    }

    /**
     * the focus element is the element in the template that is editable.  The aux elements are the extra panels that provide context.
     * The resulting transform will be applied to the aux element
     * @param rotFocus
     * @param dyFocus
     * @param rotAux
     * @param dyAux
     * @return
     */
    public String jiggerTransform(double rotFocus, double dyFocus, double rotAux, double dyAux, double width)
    {
        String transform = "rotate(" + rotAux + ") translate(0," + dyAux + ")";
        transform = " translate(" +(-0*width)+ ","+-dyFocus+") rotate(" +-rotFocus+ ") "+transform;
        return transform;
    }

    public String templateForRing(int ring)
    {
        Ring ring1 = rings.get(ring);

        MandalaPanel delegate = ring1.mPanel.getDelegate();
        double w,h;
        if (delegate instanceof SVGMandalaPanel) {
            SVGMandalaPanel svgPanel = (SVGMandalaPanel)delegate;
            w = svgPanel.width;
            h = svgPanel.height;
        } else {
            w = ring1.width;
            h = ring1.height;
        }

        //

        StringBuilder rval = new StringBuilder();

        rval .append(svgHeader(w, h));

        rval.append("<g transform=\"translate(" +(0.5*w)+ ",0)\">\n");

        double rot1 = degreesForPanel(ring1, 0);
        double dy1 = yOffsetForRing(ring1);


        if (ring+1< rings.size()) {
            rval.append("<g>\n");
            int idx2 = ring + 1;
            Ring ring2 = rings.get(idx2);
            double hAlign = 0.5;
            double vAlign = 0;

            for (int j=-2; j<=2; j++) {
                double rot0 = degreesForPanel(ring2, j);
                double dy0 = yOffsetForRing(ring2);

                String transform = jiggerTransform(rot1, dy1, rot0, dy0, w);
                rval.append("<g transform=\" " + transform + "\">\n");
                rval.append(ring2.mPanel.toSVG(ring2.width, ring2.height, hAlign, vAlign));
                rval.append("\n</g>\n");
            }
            rval.append("\n</g>\n");
        }

        rval.append("<g>\n");
        rval.append("<use transform=\"" +jiggerTransform(rot1, dy1, degreesForPanel(ring1, -1), dy1, w)+ "\"" +
            " xlink:href=\"#panel\"/>\n");
        rval.append("  <g id=\"panel\"" +
            " transform=\"" +jiggerTransform(rot1, dy1, degreesForPanel(ring1, 0), dy1, w)+"\""+
            ">\n");
        rval.append( ring1.mPanel.toSVG(w, h, 0.5, 0) );
        rval.append("\n  </g>\n");
        rval.append("<use transform=\"" +jiggerTransform(rot1, dy1, degreesForPanel(ring1, 1), dy1, w)+ "\"" +
            " xlink:href=\"#panel\"/>\n");
        rval.append("\n</g>\n");


        if (ring>0) {
            rval.append("<g>\n");
            int idx0 = ring - 1;
            Ring ring0 = rings.get(idx0);
            double hAlign, vAlign;
            hAlign = 0.5;
            if (idx0 == 0) {
                vAlign = 0.5;
            } else {
                vAlign = 0;
            }

            for (int j=-2; j<=2; j++) {
                double rot0 = degreesForPanel(ring0, j);
                double dy0 = yOffsetForRing(ring0);

                String transform = jiggerTransform(rot1, dy1, rot0, dy0, w);
                rval.append("<g transform=\" " + transform + "\">\n");
                rval.append(ring0.mPanel.toSVG(ring0.width, ring0.height, hAlign, vAlign));
                rval.append("\n</g>\n");
            }
            rval.append("\n</g>\n");
        }

        rval.append("</g>\n");
        rval.append("\n</svg>\n");

        return rval.toString();
    }

    public static class Ring
    {
        double phase;
        int count;
        double radius;
        PlaceholderPanel mPanel;
        double width;
        double height;

        public Ring(double phase, int count, PlaceholderPanel mPanel,
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
            MandalaPanel delegate = getDelegate();

            if (delegate !=null) {
                return delegate.toSVG(width, height, hAlign, vAlign);
            }

            return placeholder(width, height, 0.5, 0.0);
        }

        public MandalaPanel getDelegate()
        {
            if (delegate == null ) {
                if (imageFile.exists()) {
                    try {
                        Tika tika = new Tika();
                        String mime = tika.detect(imageFile);
                        if ("image/svg+xml".equals(mime)) {
                            this.delegate = extractArtFromSVG();
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
            return delegate;
        }

        public MandalaPanel extractArtFromSVG()
            throws IOException
        {

            try {
                SAXBuilder builder  = new SAXBuilder();
                Document doc = builder.build(imageFile);
                Element root = doc.getRootElement();

                double width = Double.parseDouble(root.getAttributeValue("width"));
                double height = Double.parseDouble(root.getAttributeValue("height"));

                String result = extractPanelElementsFromSVG(root);
                return new SVGMandalaPanel(result, width, height);
            } catch (Exception e) {
                logger.warn("failed to parse "+ imageFile, e);

                String result;
                result = Util2.slurp(new FileReader(imageFile));
                return new MandalaPanel()
                {
                    @Override
                    public String toSVG(double width, double height, double hAlign, double vAlign)
                    {
                        return result;
                    }
                };
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

        public void resetPanel()
        {
            delegate = null;
        }

    }

    public static class SVGMandalaPanel
        implements MandalaPanel
    {
        private final String payload;
        public final double width;
        public final double height;

        public SVGMandalaPanel(String payload, double width, double height)
        {
            this.payload = payload;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toSVG(double width, double height, double hAlign, double vAlign)
        {
            StringBuilder rval = new StringBuilder();

            double x = -hAlign * width;
            double y = -vAlign * height;
            rval.append("<g transform=\"translate(" + x + "," + y + ")\">\n");
            rval.append(payload);
            rval.append("</g>");
            return rval.toString();
        }
    }

    public static String extractPanelElementsFromSVG(Element root)
    {
        List<Element> children = root.getChildren();

        XMLOutputter xout = new XMLOutputter();
        StringBuilder rval = new StringBuilder();

        for (Element child : children) {
            if ("defs".equals(child.getName()))
                continue;
            if ("namedview".equals(child.getName()))
                continue;
            if ("metadata".equals(child.getName()))
                continue;
            rval.append(xout.outputString(child));
        }

        return rval.toString();
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
