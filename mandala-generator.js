SVG_NS="http://www.w3.org/2000/svg"
XLINK_NS = "http://www.w3.org/1999/xlink"


function placeholderElementForRing(ring, w, h)
{
    var msg = String.fromCharCode(0x41+ring)
    var text = document.createElementNS(SVG_NS, "text")
    text.appendChild(document.createTextNode(msg))
    var points=20
    text.setAttribute("style", "fill:#000; stroke:none; font-size:"+points+"px; font-family:serif")
    text.style.textAnchor = "middle"
    text.setAttribute("y", points/2)

    var g = document.createElementNS(SVG_NS, "g")
    if (0) {
	var rect = document.createElementNS(SVG_NS, "rect")
	rect.setAttribute("width", w)
	rect.setAttribute("height", h)
	rect.setAttribute("x", -w/2)
	rect.setAttribute("y", -h/2)
	rect.style.fill="#fff"
	rect.style.stroke="#00c"
	g.appendChild(rect)
    }  {
	var circle = document.createElementNS(SVG_NS, "ellipse")
	circle.setAttribute("rx", w/2)
	circle.setAttribute("ry", h/2)
	circle.style.fill="#fff"
	circle.style.stroke="#00c"
	g.appendChild(circle)
    }
    
    g.appendChild(text)
    g.setAttribute("id", "element"+ring)

    return g
}

function nextRingCount(oldCount, minCount)
{
    if (oldCount >= minCount)
	return oldCount
    
    if (oldCount%2==0) {
	var count = oldCount*3/2
	if (count>= minCount)
	    return count
    }

    var count = oldCount*2
    if (count>=minCount)
	return count

    return minCount
}

function widthForRing(ring)
{
    if (ring<4)
	return 60
    else
	return 80
}

function phaseForRing(ring)
{
    if (ring%2==0) {
	return 0
    } else {
	return 0.5
    }
}

function radiusForRing(ring)
{
    return (ring+1)*50
}

function mandala_test_pattern(parent)
{
    var nRings = 8;
    var h = 80

    var oldCount=0
    var counts = []
    for (var ring=0; ring<nRings; ring++) {
	var r = radiusForRing(ring)
	var w = widthForRing(ring)
	var phase = phaseForRing(ring)

	var minCount = Math.ceil((r+h/2)*2*Math.PI / (w))
	var count = nextRingCount(oldCount, minCount)
	counts.push(count)
	oldCount = count
    }

    
    for (var ring=nRings-1; ring>=0; ring--) {

	var r = radiusForRing(ring)
	var w = widthForRing(ring)
	var phase = phaseForRing(ring)

	var count=counts[ring]

	console.log("ring="+ring+" count="+count)
	for (j=0; j<=count; j++) {
	    var theta = 360 * (j+phase) / count
	    var g = document.createElementNS(SVG_NS, "g")
	    g.setAttribute("transform", "rotate("+theta.toFixed(3)+") translate(0,"+r+")")
	    g.style.visibility = "inherit"
	    
	    if (j==0) {
		var elt = placeholderElementForRing(ring, w,h)
		ringId = elt.getAttribute("id")
		g.appendChild(elt)
	    } else {
		var use = document.createElementNS(SVG_NS, "use")
		use.setAttributeNS(XLINK_NS, "href", "#"+ringId)
		if (j==count) {
		    var clipId = "wrap_clip_"+ring
		    use.setAttribute("clip-path", "url(#"+clipId+")")
		    var clip = document.createElementNS(SVG_NS, "clipPath")
		    var rect = document.createElementNS(SVG_NS, "rect")
		    rect.setAttribute("x", 0)
		    rect.setAttribute("y", -h)
		    rect.setAttribute("width", w)
		    rect.setAttribute("height", h*2)
		    clip.setAttribute("id", clipId)
		    //clip.style.visibility = "hidden"
		    clip.append(rect)
		    g.appendChild(clip)
		} 
		//if (j==count)
		g.appendChild(use)

	    }

	    parent.appendChild(g)
	}

	oldCount = count
    }

    var circ = document.createElementNS(SVG_NS, "circle")
    circ.setAttribute("cx", 0)
    circ.setAttribute("cy", 0)
    circ.setAttribute("r", 50)

    parent.appendChild(circ)

}
