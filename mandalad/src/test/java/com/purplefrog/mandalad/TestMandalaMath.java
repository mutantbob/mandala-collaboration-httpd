package com.purplefrog.mandalad;

import junit.framework.*;

public class TestMandalaMath
    extends TestCase
{
    public void test1()
    {
        MandalaConfig.SVGMandalaPanel panel = new MandalaConfig.SVGMandalaPanel("", 200, 200, null);

        assertEquals(0, panel.yOffsetFor(100, 0.0), 1e-6);
    }

    public void test2()
    {
        MandalaConfig.SVGMandalaPanel panel = new MandalaConfig.SVGMandalaPanel("", 200, 200, null);

        assertEquals(-100, panel.yOffsetFor(100, 0.5), 1e-6);
    }

    public void test3()
    {
        MandalaConfig.SVGMandalaPanel panel = new MandalaConfig.SVGMandalaPanel("", 200, 200, null);

        assertEquals(0, panel.yOffsetFor(120, 0.0), 1e-6);
    }

    public void test4()
    {
        MandalaConfig.SVGMandalaPanel panel = new MandalaConfig.SVGMandalaPanel("", 200, 200, null);

        assertEquals(-100, panel.yOffsetFor(120, 0.5), 1e-6);
    }
}
