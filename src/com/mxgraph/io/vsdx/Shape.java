/**
 * Copyright (c) 2006-2016, JGraph Ltd
 * Copyright (c) 2006-2016, Gaudenz Alder
 */
package com.mxgraph.io.vsdx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mxgraph.io.mxVsdxCodec;

public class Shape extends Style
{
	/**
	 * The text element of the shape, if any
	 */
	protected Element text;
	
	protected double width = 0;
	
	protected double height = 0;
	protected double lastX = 0;
	
	protected double lastY = 0;
	
	protected double lastMoveX = 0;
	
	protected double lastMoveY = 0;

	protected double lastKnot = -1;
	
	protected List<Element> geom;
	
	protected Map<String, String> imageData;

	public mxPathDebug debug = null;

	public Shape(Element shape, mxVsdxModel model)
	{
		super(shape, model);
		this.width = getScreenNumericalValue(this.cellElements.get(mxVsdxConstants.WIDTH), 0);
		this.height = getScreenNumericalValue(this.cellElements.get(mxVsdxConstants.HEIGHT), 0);
	}

	/**
	 * Caches the specified element
	 * @param elem the element to cache
	 */
	protected void parseShapeElem(Element elem, mxVsdxModel model)
	{
		super.parseShapeElem(elem, model);
		
		String childName = elem.getNodeName();

		if (childName.equals("ForeignData"))
		{
			String filename = elem.getOwnerDocument().getDocumentURI();
			String iType = elem.getAttribute("ForeignType");
			String compression = elem.getAttribute("CompressionType");
			
			if (iType.equals("Bitmap"))
			{
				compression = compression.toLowerCase();
			}
			else if (iType.equals("MetaFile"))
			{
				compression = "x-wmf";
			}
			else if (iType.equals("Enhanced Metafile"))
			{
				compression = "x-emf";
			}
			else
			{
				//TODO log and unsupported type
				return;
			}
			
			Node fdChild = elem.getFirstChild();
			
			if (fdChild != null)
			{
				if (fdChild instanceof Element)
				{
					Element fdElem = (Element) fdChild;
					String grandchildName = fdElem.getNodeName();
					
					if (grandchildName.toLowerCase().equals("rel"))
					{
						String rid = fdElem.getAttribute("r:id");
						
						if (rid != null && !rid.isEmpty())
						{
							// insert "_rel" into the path
							int index = filename.lastIndexOf('/');
							String pre = "";
							String post = "";

							try
							{
								pre = filename.substring(0, index);
								post = filename.substring(index, filename.length());
							}
							catch (IndexOutOfBoundsException e)
							{
								return;
							}
							
							Element relElem = model.getRelationship(rid, pre + "/_rels" + post + ".rels");
							
							if (relElem != null)
							{
								String target = relElem.getAttribute("Target");
								String type = relElem.getAttribute("Type");
								index = target.lastIndexOf('/');
								
								try
								{
									target = target.substring(index + 1, target.length());
								}
								catch (IndexOutOfBoundsException e)
								{
									return;
								}
								
								if (type != null && type.endsWith("image"))
								{
									this.imageData = new HashMap<String, String>();
									this.imageData.put("iData", model.getMedia(mxVsdxCodec.vsdxPlaceholder + "/media/" + target));
									this.imageData.put("iType", compression);
								}
							}
							else
							{
								//TODO log path issue
							}
							
							// more than one rel would break things
							return;
						}
						

					}
				}
				
				fdChild = fdChild.getNextSibling();
			}
		}
		else if (childName.equals(mxVsdxConstants.TEXT))
		{
			this.text = elem;
		}
	}

	/**
	 * Caches the specific section element
	 * @param elem the element to cache
	 */
	protected void parseSection(Element elem)
	{
		String n = elem.getAttribute("N");
		
		if (n.equals("Geometry"))
		{
			if (geom == null)
			{
				geom = new ArrayList<Element>();
			}

			this.geom.add(elem);
		}
		else
		{
			super.parseSection(elem);
		}
	}

	/**
	 * 
	 * @return mxGraph stencil XML or null or there is no displayed geometry
	 */
	protected String parseGeom()
	{
		if (!hasGeom())
		{
			return "";
		}
		
		String parsedGeom = "";
		double h = this.getHeight();
		double w = this.getWidth();
		double x = 0, y = 0;
		
		for (int i = 0; i < geom.size(); i++)
		{
			boolean noFill = false;
			boolean noLine = false;
			boolean noShow = false;
			boolean noSnap = false;
			String geomElemParsed = "";

			Node child = geom.get(i).getFirstChild();
			
			while (child != null)
			{
				if (child instanceof Element)
				{
					Element childElem = (Element) child;
					String childName = childElem.getNodeName();
					String value = null;
					
					if (childName.equals("Cell"))
					{
						childName = childElem.getAttribute("N");
						value = childElem.getAttribute("V");
					}
					else if (childName.equals("Row"))
					{
						childName = childElem.getAttribute("T");
					}
					else
					{
						value = childElem.getTextContent();
					}
					
					switch (childName)
					{
						case "NoFill":
							if (value != null && value.equals("1"))
							{
								noFill = true;
							}
							break;
						case "NoLine":
							if (value != null && value.equals("1"))
							{
								noLine = true;
							}
							break;
						case "NoShow":
							if (value != null && value.equals("1"))
							{
								noShow = true;
							}
							break;
						case "NoSnap":
							if (value != null && value.equals("1"))
							{
								noSnap = true;
							}
							break;
						case "MoveTo":
							Map <String, String> children = getChildValues(childElem, null);
							String xValue = children.get("X");
							String yValue = children.get("Y");
								
							if (xValue != null && yValue != null)
							{
								x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
								y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
							}
							
							x = x * 100.0 / w;
							y = y * 100.0 / h;
							y = 100 - y;

							x = Math.round(x * 100.0) / 100.0;
							y = Math.round(y * 100.0) / 100.0;
							
							this.lastX = x;
							this.lastY = y;
							this.lastMoveX = x;
							this.lastMoveY = y;

							geomElemParsed += "<" + "move" + " x=\"" + String.valueOf(x) + "\" y=\"" + String.valueOf(y) + "\"/>";
							break;
						case "RelMoveTo":
							children = getChildValues(childElem, null);
							xValue = children.get("X");
							yValue = children.get("Y");
								
							if (xValue != null && yValue != null)
							{
								x = Double.parseDouble(xValue) * 100;
								y = 100 - Double.parseDouble(yValue) * 100;
							}
							
							x = Math.round(x * 100.0) / 100.0;
							y = Math.round(y * 100.0) / 100.0;
							
							this.lastX = x;
							this.lastY = y;
							this.lastMoveX = x;
							this.lastMoveY = y;

							geomElemParsed += "<" + "move" + " x=\"" + String.valueOf(x) + "\" y=\"" + String.valueOf(y) + "\"/>";
							break;
						case "LineTo":
							children = getChildValues(childElem, null);
							xValue = children.get("X");
							yValue = children.get("Y");
								
							if (xValue != null && yValue != null)
							{
								x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
								y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
							}
	
							x = x * 100.0 / w;
							y = y * 100.0 / h;
							y = 100 - y;

							x = Math.round(x * 100.0) / 100.0;
							y = Math.round(y * 100.0) / 100.0;
							
							this.lastX = x;
							this.lastY = y;

							geomElemParsed += "<" + "line" + " x=\"" + String.valueOf(x) + "\" y=\"" + String.valueOf(y) + "\"/>";
							break;
						case "RelLineTo":
							children = getChildValues(childElem, null);
							xValue = children.get("X");
							yValue = children.get("Y");
								
							if (xValue != null && yValue != null)
							{
								x = Double.parseDouble(xValue) * 100;
								y = 100 - Double.parseDouble(yValue) * 100;
							}
							
							x = Math.round(x * 100.0) / 100.0;
							y = Math.round(y * 100.0) / 100.0;
							
							this.lastX = x;
							this.lastY = y;

							geomElemParsed += "<" + "line" + " x=\"" + String.valueOf(x) + "\" y=\"" + String.valueOf(y) + "\"/>";
							break;
						case "NURBSTo":
							geomElemParsed += nurbsPath(childElem, "curve");
							break;
						case "ArcTo":
							geomElemParsed += arcPath(childElem, "arc");
							break;
						case "InfiniteLine":
							//xyElem(childElem, "line");
							break;
						case "Ellipse":
							geomElemParsed += ellipsePath(childElem, "ellipse");
							break;
						case "EllipticalArcTo":
							geomElemParsed += ellArcPath(childElem, "arc");
							break;
						case "SplineStart":
							geomElemParsed += splineStartPath(childElem, "splineStart");
							break;
						case "SplineKnot":
							geomElemParsed += splinePath(childElem, "splineKnot");
							break;
						case "PolylineTo":
							geomElemParsed += polyPath(childElem, "poly");
							break;
						default:
							this.styleDebug("ERROR: geom type not understood  - " + childName);
							break;
							
					}
				}

				child = child.getNextSibling();
			}
			
			if (!noShow && !geomElemParsed.equals(""))
			{
				if (noFill)
				{
					geomElemParsed += "</path><stroke/>";
				}
				else
				{
					geomElemParsed += "</path><fillstroke/>";
				}
				
				parsedGeom += "<path>" + geomElemParsed;
			}
		}

		if (parsedGeom.equals(""))
		{
			return "";
		}
		
		//System.out.println(parsedGeom);

		return "<shape strokewidth=\"inherit\"><foreground>" + parsedGeom + "</foreground></shape>";
	}

	protected String arcPath(Element arcElem, String command)
	{
		Map <String, String> children = getChildValues(arcElem, null);
		String xValue = children.get("X");
		String yValue = children.get("Y");
		String aValue = children.get("A");
			
		if (xValue != null && yValue != null && aValue != null)
		{
			double h = this.getHeight();
			double w = this.getWidth();
			double x0 = Math.round(this.lastX * w) / 100;
			double y0 = Math.round(this.lastY * h) / 100;
			double x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
			
			double y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
			y = h - y;
			
			double a = Double.parseDouble(aValue) * mxVsdxUtils.conversionFactor;

			double dx = Math.abs(x - x0);
			double dy = Math.abs(y - y0);

			double rx = (a * 0.5) + (dx * dx + dy * dy) / (8.0 * a);
			double ry = rx;
			double r0 = Math.abs(rx);
			
			rx = rx * 100 / w;
			ry = ry * 100 / h;
			x = x * 100 / w;
			y = y * 100 / h;
			rx = Math.round(rx * 100.0) / 100.0;
			ry = Math.round(ry * 100.0) / 100.0;
			x = Math.round(x * 100.0) / 100.0;
			y = Math.round(y * 100.0) / 100.0;

			a = Math.round(a * 100.0) / 100.0;
			rx = Math.abs(rx);
			ry = Math.abs(ry);
			
			//determine sweep and large-arc flag
			String sf = (a < 0) ? "1" : "0";
			String laf = (r0 < Math.abs(a)) ? "1" : "0";

			if (debug != null)
			{
				debug.drawLine(x0, y0, x, y, "");
			}
			
			this.lastX = x;
			this.lastY = y;

			return "<" + command + 
					" rx=\"" + String.valueOf(rx) + 
					"\" ry=\"" + String.valueOf(ry) + 
					"\" x=\"" + String.valueOf(x) + 
					"\" y=\"" + String.valueOf(y) + 
					"\" x-axis-rotation=\"0" + 
					"\" large-arc-flag=\"" + laf + 
					"\" sweep-flag=\"" + sf + 
					"\"/>";
		}
		
		return "";
	}
	
	protected String ellipsePath(Element ellipseElem, String command)
	{
		Map <String, String> children = getChildValues(ellipseElem, null);
		String xValue = children.get("X");
		String yValue = children.get("Y");
		String aValue = children.get("A");
		String bValue = children.get("B");
		String cValue = children.get("C");
		String dValue = children.get("D");

		if (xValue != null && yValue != null && aValue != null && bValue != null && cValue != null && dValue != null)
		{
			double h = this.getHeight();
			double w = this.getWidth();
			double x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
			double y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
			y = h - y;
			double a = Double.parseDouble(aValue) * mxVsdxUtils.conversionFactor;
			double b = Double.parseDouble(bValue) * mxVsdxUtils.conversionFactor;
			b = h - b;
			double c = Double.parseDouble(cValue) * mxVsdxUtils.conversionFactor;
			double d = Double.parseDouble(dValue) * mxVsdxUtils.conversionFactor;
			d = h - d;
			
			double dx1 = Math.abs(a - x);
			double dy1 = Math.abs(b - y);
			double r1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);

			double dx2 = Math.abs(c - x);
			double dy2 = Math.abs(d - y);
			double r2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
			double newX = (x - r1) * 100 / w;
			double newY = (y - r2) * 100 / h;
			double newW = 2 * r1 * 100 / w;
			double newH = 2 * r2 * 100 / h;
			newH = Math.round(newH * 100.0) / 100.0;
			newW = Math.round(newW * 100.0) / 100.0;
			newX = Math.round(newX * 100.0) / 100.0;
			newY = Math.round(newY * 100.0) / 100.0;
			
			return "<" + command + 
					" x=\"" + String.valueOf(newX) + 
					"\" y=\"" + String.valueOf(newY) + 
					"\" w=\"" + String.valueOf(newW) + 
					"\" h=\"" + String.valueOf(newH) + 
					"\"/>";
		}
		
		return "";
	}
	
	protected String ellArcPath(Element ellArcElem, String command)
	{
		Map <String, String> children = getChildValues(ellArcElem, null);
		String xValue = children.get("X");
		String yValue = children.get("Y");
		String aValue = children.get("A");
		String bValue = children.get("B");
		String cValue = children.get("C");
		String dValue = children.get("D");
		
		if (xValue != null && yValue != null && aValue != null && bValue != null && cValue != null && dValue != null)
		{
			double h = this.getHeight();
			double w = this.getWidth();
			double x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
			double y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
			y = h - y;
			double a = Double.parseDouble(aValue) * mxVsdxUtils.conversionFactor;
			double b = Double.parseDouble(bValue) * mxVsdxUtils.conversionFactor;
			double c = Double.parseDouble(cValue);
			double d = Double.parseDouble(dValue);
			
			x = x * 100.0 / w;
			y = y * 100.0 / h;
			
			double x1 = this.lastX * w / 100.0;
			double y1 = this.lastY * h / 100.0;
			
			double x2 = x * w / 100.0;
			double y2 = y * h / 100.0;
			
			double x3 = a;
			double y3 = h - b;

			double ang = -c;
			
			double p1x = Math.sqrt(x1 * x1 + y1 * y1) * Math.cos(Math.atan2(y1, x1) - ang);
			double p1y = Math.sqrt(x1 * x1 + y1 * y1) * Math.sin(Math.atan2(y1, x1) - ang);
            
			double p2x = Math.sqrt(x2 * x2 + y2 * y2) * Math.cos(Math.atan2(y2, x2) - ang);
			double p2y = Math.sqrt(x2 * x2 + y2 * y2) * Math.sin(Math.atan2(y2, x2) - ang);
            
			double p3x = Math.sqrt(x3 * x3 + y3 * y3) * Math.cos(Math.atan2(y3, x3) - ang);
			double p3y = Math.sqrt(x3 * x3 + y3 * y3) * Math.sin(Math.atan2(y3, x3) - ang);
			
			double p0x = ((p1x-p2x)*(p1x+p2x)*(p2y-p3y)-(p2x-p3x)*(p2x+p3x)*(p1y-p2y)+d*d*(p1y-p2y)*(p2y-p3y)*(p1y-p3y))/(2*((p1x-p2x)*(p2y-p3y)-(p2x-p3x)*(p1y-p2y)));
			double p0y = ((p1x-p2x)*(p2x-p3x)*(p1x-p3x)/(d*d)+(p2x-p3x)*(p1y-p2y)*(p1y+p2y)-(p1x-p2x)*(p2y-p3y)*(p2y+p3y))/(2*((p2x-p3x)*(p1y-p2y)-(p1x-p2x)*(p2y-p3y)));
			
			double newX = Math.sqrt(p0x * p0x + p0y * p0y) * Math.cos(Math.atan2(p0y, p0x) + ang);
			double newY = Math.sqrt(p0x * p0x + p0y * p0y) * Math.sin(Math.atan2(p0y, p0x) + ang);
			
			newX = newX * w / 100.0;
			newY = newY * h / 100.0;
			
			double dx = p1x - p0x;
			double dy = p1y - p0y;
			double rx = Math.sqrt(dx * dx + dy * dy * d * d);
			double ry = rx / d;
			double rot = Math.toDegrees(ang);
			
			rx = rx * 100.0 / w;
			ry = ry * 100.0 / h;
			
			x = Math.round(x * 100.0) / 100.0;
			y = Math.round(y * 100.0) / 100.0;
			rx = Math.round(rx * 100.0) / 100.0;
			ry = Math.round(ry * 100.0) / 100.0;
			rot = Math.round(rot * 100.0) / 100.0;

			//determine sweep
			//TODO fix rare error (file "1 Supported Forms" shape "storeddata" on page 5)
			double sweep = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1); 
			String sf = (sweep > 0) ? "0" : "1"; 
			
			//determine large arc flag
			String laf = "0";

			if (mxVsdxUtils.isInsideTriangle(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y) && 
					isReflexAngle(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y))
			{
				laf = "1";
			}
			
			if (debug != null)
			{
				debug.drawRect(p0x, p0y, "P0");
				debug.drawRect(p1x, p1y, "P1");
				debug.drawRect(p2x, p2y, "P2");
				debug.drawRect(p3x, p3y, "P3");
				debug.drawRect(newX, newY, "X");
				debug.drawRect(x3, y3, "CP");
				debug.drawLine(x1, y1, x2, y2, "");
			}
			
			this.lastX = x;
			this.lastY = y;
			
			return "<" + command + 
			" rx=\"" + String.valueOf(rx) + 
			"\" ry=\"" + String.valueOf(ry) + 
			"\" x=\"" + String.valueOf(x) + 
			"\" y=\"" + String.valueOf(y) + 
			"\" x-axis-rotation=\"" + String.valueOf(rot) + 
			"\" large-arc-flag=\"" + laf + 
			"\" sweep-flag=\"" + sf + 
			"\"/>";
		}
		
		return "";
	}

	/**
	 * @param x0 y0 center point of ellipse containing the arc
	 * @param x1 y1 starting point of the arc
	 * @param x2 y2 endpoint of the arc
	 * @param x3 y3 control point
	 * @return true if the start to end angle that contains the control point is a reflex angle 
	 */
	private boolean isReflexAngle(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3)
	{
		x1 = x1 - x0;
		y1 = y1 - y0;
		x2 = x2 - x0;
		y2 = y2 - y0;
		x2 = x3 - x0;
		y3 = y3 - y0;
		x0 = 0;
		y0 = 0;

		double aStart = Math.toDegrees(Math.atan2(y1, x1) - Math.atan2(y0, x0));
		double aEnd = Math.toDegrees(Math.atan2(y2, x2) - Math.atan2(y0, x0));
		double aCP = Math.toDegrees(Math.atan2(y3, x3) - Math.atan2(y0, x0));
		
		aStart = (aStart - aCP) % 360;
		aEnd = (aEnd - aCP) % 360;

		if (aStart > 180)
		{
			aStart = aStart - 360;
		}
		else if (aStart < -180)
		{
			aStart = aStart + 360;
		}
		
		if (aEnd > 180)
		{
			aEnd = aEnd - 360;
		}
		else if (aEnd < -180)
		{
			aEnd = aEnd + 360;
		}
		
		if ((aStart > 0 && aEnd < 0) || (aStart < 0 && aEnd > 0))
		{
			if (Math.abs(aStart - aEnd) > 180)
			{
				return true;
			}
		}
		
		return false;
	}

	protected String polyPath(Element polyElem, String command)
	{
		Map <String, String> nodeValues = new HashMap<String, String>();
		nodeValues.put("A", "F");
		Map <String, String> children = getChildValues(polyElem, nodeValues);
		String xValue = children.get("X");
		String yValue = children.get("Y");
		String aValue = children.get("A");
		String result = "";
		
		if (xValue != null && yValue != null && aValue != null)
		{
			double h = this.getHeight();
			double w = this.getWidth();
			double x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
			double y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
			x = x * 100.0 / w;
			y = y * 100.0 / h;
			y = 100 - y;
			x = Math.round(x * 100.0) / 100.0;
			y = Math.round(y * 100.0) / 100.0;
			
			aValue = aValue.replaceAll("\\s","").toLowerCase().replaceAll("polyline\\(","").replaceAll("\\)", "");
			
			LinkedList<String> polyEntriesList = new LinkedList<String>(Arrays.asList(aValue.split(",")));
			
			polyEntriesList.remove(0);
			polyEntriesList.remove(0);
			double currX = 0;
			double currY = 0;

			while (polyEntriesList.size() > 0)
			{
				currX = Double.valueOf(polyEntriesList.remove(0)) * mxVsdxUtils.conversionFactor;
				currY = Double.valueOf(polyEntriesList.remove(0)) * mxVsdxUtils.conversionFactor;
				currY = 100 - currY;
				
				currX = Math.round(currX * 100.0) / 100.0;
				currY = Math.round(currY * 100.0) / 100.0;

				this.lastX = currX;
				this.lastY = currY;
				
				result += "<line x=\"" + String.valueOf(currX) + "\" y=\"" + String.valueOf(currY) + "\"/>";
			}

			if (this.lastMoveX == x && this.lastMoveY == y)
			{
				result += "<close/>";
			}
		}
		
		return result;
	}

	protected String splineStartPath(Element splineElem, String command)
	{
		Map <String, String> children = getChildValues(splineElem, null);
		String xValue = children.get("X");
		String yValue = children.get("Y");
		String aValue = children.get("A");
		String bValue = children.get("B");
		String cValue = children.get("C");
		String dValue = children.get("D");
		String result = "";
		
		if (xValue != null && yValue != null && aValue != null && bValue != null && cValue != null && dValue != null)
		{
			double h = this.getHeight();
			double w = this.getWidth();
			double x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
			double y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
			//double a = Double.parseDouble(aValue);
			//double b = Double.parseDouble(bValue);
			double c = Double.parseDouble(cValue);
			int d = Integer.parseInt(dValue);

			//double firstKnot = b;
			//double secondKnot = a;
			double lastKnot = c;
			this.lastKnot = lastKnot;
			int degree = d;
//				x = x * 100.0 / w;
//				y = y * 100.0 / h;
			y = 100 - y;
			x = Math.round(x * 100.0) / 100.0;
			y = Math.round(y * 100.0) / 100.0;
			lastKnot = Math.round(lastKnot * 100.0) / 100.0;
			double x0 = this.lastX * w / 100.0;
			double y0 = this.lastY * h / 100.0;
			
			result = "<curve ";

			if (debug != null)
			{
				debug.drawRect(x0, y0 , "0, " + Integer.toString(degree));
				debug.drawRect(x, y , Double.toString(lastKnot));
				debug.drawLine(x0, y0, x, y, "");
			}

			this.lastX = x;
			this.lastY = y;

		}
		
		return result;
	}

	protected String splinePath(Element splineElem, String command)
	{
		Map <String, String> children = getChildValues(splineElem, null);
		String xValue = children.get("X");
		String yValue = children.get("Y");
		String aValue = children.get("A");
		String result = "";
		
		if (xValue != null && yValue != null && aValue != null)
		{
			//double h = this.getHeight();
			//double w = this.getWidth();
			double x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
			double y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
			double a = Double.parseDouble(aValue);

			double knot = a;
//				x = x * 100.0 / w;
//				y = y * 100.0 / h;
			y = 100 - y;
			x = Math.round(x * 100.0) / 100.0;
			y = Math.round(y * 100.0) / 100.0;
			knot = Math.round(knot * 100.0) / 100.0;
			

			if (debug != null)
			{
				debug.drawRect(x, y, Double.toString(knot));
				debug.drawLine(this.lastX, this.lastY, x, y, "");
			}
			
			this.lastX = x;
			this.lastY = y;
		}
		
		return result;
	}

	protected String nurbsPath(Element nurbsElem, String command)
	{
		Map <String, String> children = getChildValues(nurbsElem, null);
		String xValue = children.get("X");
		String yValue = children.get("Y");
		String eValue = children.get("E");
		String result = "";

		if (xValue != null && yValue != null && eValue != null)
		{
			double h = this.getHeight();
			double w = this.getWidth();
			double x = Double.parseDouble(xValue) * mxVsdxUtils.conversionFactor;
			double y = Double.parseDouble(yValue) * mxVsdxUtils.conversionFactor;
			eValue = eValue.replace("NURBS(", "");
			eValue = eValue.replace(")", "");
			
			List<String> nurbsValues = Arrays.asList(eValue.split("\\s*,\\s*"));
			
			if (nurbsValues.size() >= 10)
			{
				double x1 = Double.parseDouble(nurbsValues.get(4)) * 100.0;
				double y1 = 100 - Double.parseDouble(nurbsValues.get(5)) * 100.0;
				double x2 = Double.parseDouble(nurbsValues.get(8)) * 100.0;
				double y2 = 100 - Double.parseDouble(nurbsValues.get(9)) * 100.0;
	
				y = y * 100.0 / h;
				x = x * 100.0 / w;
				y = 100 - y;
				x = Math.round(x * 100.0) / 100.0;
				y = Math.round(y * 100.0) / 100.0;
				x1 = Math.round(x1 * 100.0) / 100.0;
				y1 = Math.round(y1 * 100.0) / 100.0;
				x2 = Math.round(x2 * 100.0) / 100.0;
				y2 = Math.round(y2 * 100.0) / 100.0;
	
				if (debug != null)
				{
					debug.drawRect(x, y, "");
					debug.drawLine(this.lastX, this.lastY, x, y, "");
				}
				
				this.lastX = x;
				this.lastY = y;
				
				result += "<curve x1=\"" + String.valueOf(x1) + "\" y1=\"" + String.valueOf(y1) + 
						      "\" x2=\"" + String.valueOf(x2) + "\" y2=\"" + String.valueOf(y2) + 
						      "\" x3=\"" + String.valueOf(x) + "\" y3=\"" + String.valueOf(y) + "\"/>";
			}
		}

		return result;
	}

	/**
	 * Returns the value of the Text element.
	 * @return Value of the Text element.
	 */
	public String getText()
	{
		return this.text != null ? text.getTextContent() : null;
	}

	/**
	 * Returns the children Nodes of Text.
	 * @return List with the children of the Text element.
	 */
	public NodeList getTextChildren()
	{
		return this.text != null ? text.getChildNodes() : null;
	}

	/**
	 * Checks if the shape has defined a width element.
	 * @return Returns <code>true</code> if the shape has defined a width element.
	 */
	public boolean hasWidth()
	{
		return hasProperty(mxVsdxConstants.X_FORM, mxVsdxConstants.WIDTH);
	}

	/**
	 * Returns the value of the width element in pixels.
	 * @return Numerical value of the width element.
	 */
	public double getWidth()
	{
		return this.width;
	}

	/**
	 * Checks if the shape has defined a height element.
	 * @return Returns <code>true</code> if the shape has defined a height element.
	 */
	public boolean hasHeight()
	{
		return hasProperty(mxVsdxConstants.X_FORM, mxVsdxConstants.HEIGHT);
	}

	/**
	 * Returns the value of the height element in pixels.
	 * @return Numerical value of the height element.
	 */
	public double getHeight()
	{
		return this.height;
	}
	
	/**
	 * Returns whether or not this shape has a geometry defined, locally
	 * or inherited
	 * @return whether the shape has a geometry
	 */
	public boolean hasGeom()
	{
		return !(this.geom == null || this.geom.isEmpty());
	}
}
