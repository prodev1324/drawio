/**
 * Copyright (c) 2006-2016, JGraph Ltd
 * Copyright (c) 2006-2016, Gaudenz Alder
 */
package com.mxgraph.io.vsdx;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mxgraph.util.mxConstants;

/**
 * This class is a general wrapper for one Shape Element.<br/>
 * Provides a set of methods for retrieving the value of different properties
 * stored in the shape element.<br/>
 * References to other shapes or style-sheets are not considered.
 */
public class Style
{
	protected Element shape;
	
	protected Integer Id;
	
	// .vsdx cells elements that contain one style each
	protected Map<String, Element> cellElements = new HashMap<String, Element>();
	
	protected Map<String, Section> sections = new HashMap<String, Section>();

	protected mxPropertiesManager pm;
	
	/**
	 * Mapping of line,text and fill styles to the style parents
	 */
	protected Map<String, Style> styleParents = new HashMap<String, Style>();
	
	protected Style textParent;
	
	protected Style theme;

	private final static Logger LOGGER = Logger.getLogger(Style.class.getName());
	
	public static boolean vsdxStyleDebug = false;
	
	protected static Map<String, String> styleTypes = new HashMap<String, String>();
	
	static
	{
		styleTypes.put(mxVsdxConstants.FILL, mxVsdxConstants.FILL_STYLE);
		styleTypes.put(mxVsdxConstants.FILL_BKGND, mxVsdxConstants.FILL_STYLE);
		styleTypes.put(mxVsdxConstants.FILL_BKGND_TRANS, mxVsdxConstants.FILL_STYLE);
		styleTypes.put(mxVsdxConstants.FILL_FOREGND, mxVsdxConstants.FILL_STYLE);
		styleTypes.put(mxVsdxConstants.FILL_FOREGND_TRANS, mxVsdxConstants.FILL_STYLE);
		styleTypes.put(mxVsdxConstants.FILL_PATTERN , mxVsdxConstants.FILL_STYLE);
		styleTypes.put(mxVsdxConstants.SHDW_PATTERN, mxVsdxConstants.FILL_STYLE);
		styleTypes.put(mxVsdxConstants.FILL_STYLE, mxVsdxConstants.FILL_STYLE);
		
		styleTypes.put(mxVsdxConstants.BEGIN_ARROW, mxVsdxConstants.LINE_STYLE);
		styleTypes.put(mxVsdxConstants.END_ARROW, mxVsdxConstants.LINE_STYLE);
		styleTypes.put(mxVsdxConstants.LINE_PATTERN, mxVsdxConstants.LINE_STYLE);
		styleTypes.put(mxVsdxConstants.LINE_COLOR, mxVsdxConstants.LINE_STYLE);
		styleTypes.put(mxVsdxConstants.LINE_COLOR_TRANS, mxVsdxConstants.LINE_STYLE);
		
		styleTypes.put(mxVsdxConstants.TEXT_BKGND, mxVsdxConstants.TEXT_STYLE);
		styleTypes.put(mxVsdxConstants.BOTTOM_MARGIN, mxVsdxConstants.TEXT_STYLE);
		styleTypes.put(mxVsdxConstants.LEFT_MARGIN, mxVsdxConstants.TEXT_STYLE);
		styleTypes.put(mxVsdxConstants.RIGHT_MARGIN, mxVsdxConstants.TEXT_STYLE);
		styleTypes.put(mxVsdxConstants.TOP_MARGIN, mxVsdxConstants.TEXT_STYLE);
		styleTypes.put(mxVsdxConstants.PARAGRAPH, mxVsdxConstants.TEXT_STYLE);
		styleTypes.put(mxVsdxConstants.CHARACTER, mxVsdxConstants.TEXT_STYLE);
	};
	
	/**
	 * Create a new instance of mxGeneralShape
	 * @param shape Shape Element to be wrapped.
	 */
	public Style(Element shape, mxVsdxModel model)
	{
		this.shape = shape;
		this.pm = model.getPropertiesManager();
		
		String Id = shape.getAttribute(mxVsdxConstants.ID);
		
		try
		{
			this.Id = (Id != null && !Id.isEmpty()) ? Integer.valueOf(Id) : -1;
		}
		catch (Exception e)
		{
			// TODO handle exception correctly
		}
		
		cacheCells(model);
		stylesheetRefs(model);
	}

	public void styleDebug(String debug)
	{
		if (vsdxStyleDebug)
		{
			System.out.println(debug);
		}
	}

	public void stylesheetRefs(mxVsdxModel model)
	{
		styleParents.put(mxVsdxConstants.FILL_STYLE, model.getStylesheet(shape.getAttribute(mxVsdxConstants.FILL_STYLE)));
		styleParents.put(mxVsdxConstants.LINE_STYLE, model.getStylesheet(shape.getAttribute(mxVsdxConstants.LINE_STYLE)));
		
		Style textStyle = model.getStylesheet(shape.getAttribute(mxVsdxConstants.TEXT_STYLE));
		styleParents.put(mxVsdxConstants.TEXT_STYLE, model.getStylesheet(shape.getAttribute(mxVsdxConstants.TEXT_STYLE)));
		
		this.textParent = textStyle;
		
		Style theme = model.getStylesheet("0");
		this.theme = theme;
	}

	/**
	 * Checks if the shape Element has a children with tag name = 'tag'.
	 * @param tag Name of the Element to be found.
	 * @return Returns <code>true</code> if the shape Element has a children with tag name = 'tag'
	 */
	protected void cacheCells(mxVsdxModel model)
	{
		if (shape != null)
		{
			NodeList children = shape.getChildNodes();
			
			if (children != null)
			{
				Node childNode = children.item(0);
				
				while (childNode != null)
				{
					if (childNode instanceof Element)
					{
						parseShapeElem((Element)childNode, model);
					}
					
					childNode = childNode.getNextSibling();
				}
			}
		}
	}
	
	/**
	 * Caches the specified element
	 * @param elem the element to cache
	 */
	protected void parseShapeElem(Element elem, mxVsdxModel model)
	{
		String childName = elem.getNodeName();

		if (childName.equals("Cell"))
		{
			this.cellElements.put(elem.getAttribute("N"), elem);
		}
		else if (childName.equals("Section"))
		{
			this.parseSection(elem);
		}
	}

	/**
	 * Caches the specific section element
	 * @param elem the element to cache
	 */
	protected void parseSection(Element elem)
	{
		Section sect = new Section(elem);
		this.sections.put(elem.getAttribute("N"), sect);
	}

	/**
	 * Checks if the 'primary' Element has a child with tag name = 'tag'.
	 * @param tag Name of the Element to be found.
	 * @return Returns <code>true</code> if the 'primary' Element has a child with tag name = 'tag'.
	 */
	protected boolean hasProperty(String nodeName, String tag)
	{
		return this.cellElements.containsKey(tag);
	}
	
	/**
	 * Returns the value of the element
	 * @param elem The element whose value is to be found
	 * @param defaultValue the value to return if there is no value attribute
	 * @return String value of the element, or the default value if no value found
	 */
	protected String getValue(Element elem, String defaultValue)
	{
		if (elem != null)
		{
			return elem.getAttribute("V");
		}
		
		return defaultValue;
	}

	/**
	 * Returns the value of the element as a double
	 * @param elem The element whose value is to be found
	 * @param defaultValue the value to return if there is no value attribute
	 * @return double value of the element, or the default value if no value found
	 */
	protected double getValueAsDouble(Element cell, double defaultValue)
	{
		if (cell != null)
		{
			String value = cell.getAttribute("V");

			if (value != null)
			{
				if (value.equals("Themed"))
				{
					return 0;
				}

				try
				{
					double parsedValue = Double.parseDouble(value);
					
					String units = cell.getAttribute("U");
					
					if (units.equals("PT"))
					{
						// Convert from points to pixels
						parsedValue = parsedValue * mxVsdxUtils.conversionFactor;
					}
					
					return Math.round(parsedValue * 100.0) / 100.0;
				}
				catch (NumberFormatException e)
				{
					e.printStackTrace();
				}
			}
		}

		return defaultValue;
	}

	//if (!tag.equals(mxVdxConstants.FILL_BKGND_TRANS) && !tag.equals(mxVdxConstants.FILL_FOREGND_TRANS) && !tag.equals(mxVdxConstants.LINE_COLOR_TRANS) && !tag.equals(mxVdxConstants.NO_LINE))

	/**
	 * Returns the value of the element as a double
	 * @param elem The element whose value is to be found
	 * @param defaultValue the value to return if there is no value attribute
	 * @return double value of the element, or the default value if no value found
	 */
	protected double getScreenNumericalValue(Element cell, double defaultValue)
	{
		if (cell != null)
		{
			String value = cell.getAttribute("V");

			if (value != null)
			{
				try
				{
					double parsedValue = Double.parseDouble(value);
					parsedValue = parsedValue * mxVsdxUtils.conversionFactor;
					
					return Math.round(parsedValue * 100.0) / 100.0;
				}
				catch (NumberFormatException e)
				{
					e.printStackTrace();
				}
			}
		}

		return defaultValue;
	}
	
	/**
	 * Returns the value of the element with tag name = 'tag' in the children
	 * of 'primary' in his double representation.<br/>
	 * @param tag Name of the Element to be found.
	 * @return Numerical value of the element.
	 */
	public String getAttribute(String parentTag, String tag, String attribute, String defaultValue)
	{
		String result = defaultValue;
		Element cell = this.cellElements.get(tag);
		
		if (cell != null)
		{
			result = cell.getAttribute(attribute);
		}

		return result;
	}
	
	/**
	 * Returns the value of the attribute of the element with tag name = 'tag' in the children
	 * of the shape element<br/>
	 * @param tag Name of the Element to be found.
	 * @return Numerical value of the element.
	 */
	public String getAttribute(String tag, String attribute, String defaultValue)
	{
		String result = defaultValue;
		Element cell = this.cellElements.get(tag);
		
		if (cell != null)
		{
			result = cell.getAttribute(attribute);
		}

		return result;
	}
	
	protected Map <String, String> getChildValues(Element parent, Map<String, String> requiredValues)
	{
		Map <String, String> result = new HashMap<String, String>();
		
		Node child = parent.getFirstChild();
		
		while (child != null)
		{
			if (child instanceof Element)
			{
				Element childElem = (Element)child;
				String childName = childElem.getNodeName();
				String name = null;
				String nodeValue = null;
				
				if (childName.equals("Cell"))
				{
					name = childElem.getAttribute("N");
					nodeValue = childElem.getAttribute("V");
				}
				else
				{
					name = childElem.getNodeName();
					nodeValue = childElem.getTextContent();
				}
				
				if (requiredValues != null)
				{
					String nodeOverride = requiredValues.get(name);
					
					if (nodeOverride != null)
					{
						nodeValue = childElem.getAttribute(nodeOverride);
					}
				}
				
				result.put(name, nodeValue);
			}
			
			child = child.getNextSibling();
		}
		
		return result;
	}

	protected Element getCellElement(String cellKey, String index, String sectKey)
	{
		Section sect = this.sections.get(sectKey);
		Element elem = null;
		boolean inherit = false;

		if (sect != null)
		{
			elem = sect.getIndexedCell(index, cellKey);
		}
		
		if (elem != null)
		{
			String form = elem.getAttribute("F");
			String value = elem.getAttribute("V");
			
			if (form != null && value != null)
			{
				if (form.equals("Inh") && value.equals("Themed"))
				{
					inherit = true;
				}
				else if (form.equals("THEMEVAL()") && value.equals("Themed") && theme != null)
				{
					// Use "no style" style
					Element themeElem = theme.getCellElement(cellKey, index, sectKey);
					
					if (themeElem != null)
					{
						return themeElem;
					}
				}
			}
		}

		if (elem == null || inherit)
		{
			String styleType = Style.styleTypes.get(sectKey);
			Style parentStyle = this.styleParents.get(styleType);
			
			if (parentStyle != null)
			{
				Element parentElem = parentStyle.getCellElement(cellKey, index, sectKey);
				
				if (parentElem != null)
				{
					// Only return if non-null. Just in case (and not sure if that's valid) there is an
					// inherit formula that doesn't resolve to anything
					return parentElem;
				}
			}
		}
		
		return elem;
	}

	/**
	 * Locates the first entry for the specified style string in the style hierarchy.
	 * The order is to look locally, then delegate the request to the relevant parent style
	 * if it doesn't exist locally
	 * @param key The key of the cell to find
	 * @return the Element that first resolves to that style key or null or none is found
	 */
	protected Element getCellElement(String key)
	{
		Element elem = this.cellElements.get(key);
		boolean inherit = false;
		
		if (elem != null)
		{
			String form = elem.getAttribute("F");
			String value = elem.getAttribute("V");
			
			if (form != null && value != null)
			{
				if (form.equals("Inh") && value.equals("Themed"))
				{
					inherit = true;
				}
				else if (form.equals("THEMEVAL()") && value.equals("Themed") && theme != null)
				{
					// Use "no style" style
					Element themeElem = theme.getCellElement(key);
					
					if (themeElem != null)
					{
						return themeElem;
					}
				}
			}
		}
		
		if (elem == null || inherit)
		{
			String styleType = Style.styleTypes.get(key);
			Style parentStyle = this.styleParents.get(styleType);
			
			if (parentStyle != null)
			{
				Element parentElem = parentStyle.getCellElement(key);
				
				if (parentElem != null)
				{
					// Only return if non-null. Just in case (and not sure if that's valid) there is an
					// inherit formula that doesn't resolve to anything
					return parentElem;
				}
			}
		}
		
		return elem;
	}

	/**
	 * Returns the line color.<br/>
	 * The property may to be defined in master shape or line stylesheet.<br/>
	 * @return hexadecimal representation of the color.
	 */
	public String getStrokeColor()
	{
		String color = "";

		if (this.getValue(this.getCellElement(mxVsdxConstants.LINE_PATTERN), "1").equals("0"))
		{
			color = "none";
		}
		else
		{
			color = this.getColor(this.getCellElement(mxVsdxConstants.LINE_COLOR));
		}

		return color;
	}

	/**
	 * Returns the shape's color.
	 * The property may to be defined in master shape or fill stylesheet.
	 * If the color is the background or the fore color, it depends on the pattern.
	 * For simple gradients and solid, returns the fore color, else return the
	 * background color.
	 * @return hexadecimal representation of the color.
	 */
	protected String getFillColor()
	{
		String fillForeColor = this.getColor(this.getCellElement(mxVsdxConstants.FILL_FOREGND));
		String fillPattern = this.getValue(this.getCellElement(mxVsdxConstants.FILL_PATTERN), "0");
		
		if (fillPattern != null && fillPattern.equals("0"))
		{
			return "none";
		}
		else
		{
			return fillForeColor;
		}
	}

	protected String getColor(Element elem)
	{
		String color = this.getValue(elem, "");

		if (!color.startsWith("#"))
		{
			color = pm.getColor(color);
		}
		
		return color;
	}

	/**
	 * The TextBkgnd cell can have any value from 0 through 24, or 255. The values 0 and 255 (visTxtBlklOpaque) both indicate a transparent text background.
	 * To enter a custom color, use the RGB or HSL function plus one—for example, RGB(255,127,255)+1. The value of a custom color is its RGB color, and RGB(r, g, b)+1, 
	 * rather than a number, will be shown in the ShapeSheet window. When used in numeric operations, custom colors have values of 25 and above.
	 * You can set the transparency of the text background color in the TextBkgndTrans cell.
	 */
	protected String getTextBkgndColor(Element elem)
	{
		String color = this.getValue(elem, "");

		if (!color.startsWith("#"))
		{
			if (color.equals("0") || color.equals("255"))
			{
				return "none";
			}

			return pm.getColor(color);
		}
		
		return color;
	}
	
	/**
	 * Checks if the line weight is defined.
	 * @return Returns <code>true</code> if the line weight is defined.
	 */
	public boolean hasLineWeight()
	{
		return hasProperty(mxVsdxConstants.LINE, mxVsdxConstants.LINE_WEIGHT);
	}

	/**
	 * Returns the line weight of the shape in pixels
	 * @return Numerical value of the LineWeight element.
	 */
	public double getLineWeight()
	{
		return getValueAsDouble(this.getCellElement(mxVsdxConstants.LINE_WEIGHT), 0);
	}

	/**
	 * Returns the level of transparency of the Shape.
	 * @return double in range (opaque = 0)..(100 = transparent)
	 */
	public double getStrokeTransparency()
	{
			return getValueAsDouble(this.getCellElement(mxVsdxConstants.LINE_COLOR_TRANS), 0);
	}

	/**
	 * Returns the NameU attribute.
	 * @return Value of the NameU attribute.
	 */
	public String getNameU()
	{
		return shape.getAttribute(mxVsdxConstants.NAME_U);
	}

	/**
	 * Returns the UniqueID attribute.
	 * @return Value of the UniqueID attribute.
	 */
	public String getUniqueID()
	{
		return shape.getAttribute(mxVsdxConstants.UNIQUE_ID);
	}

	/**
	 * Returns the value of the Id attribute.
	 * @return Value of the Id attribute.
	 */
	public Integer getId()
	{
		return this.Id;
	}

	/**
	 * Returns the color of one text fragment
	 * @param charIX IX attribute of Char element
	 * @return Text color in hexadecimal representation.
	 */
	public String getTextColor(String index)
	{
		Element colorElem = getCellElement(mxVsdxConstants.COLOR, index, mxVsdxConstants.CHARACTER);
		String color = getValue(colorElem, "#000000");

		if (!color.startsWith("#"))
		{
			color = pm.getColor(color);
		}

		return color;
	}

	/**
	 * Returns the top margin of text in pixels.
	 * @return Numerical value of the TopMargin element
	 */
	public double getTextTopMargin()
	{
		return getScreenNumericalValue(this.getCellElement(mxVsdxConstants.TOP_MARGIN), 0);
	}

	/**
	 * Returns the bottom margin of text in pixels.
	 * @return Numerical value of the BottomMargin element.
	 */
	public double getTextBottomMargin()
	{
		return getScreenNumericalValue(this.getCellElement(mxVsdxConstants.BOTTOM_MARGIN), 0);
	}

	/**
	 * Returns the left margin of text in pixels.
	 * @return Numerical value of the LeftMargin element.
	 */
	public double getTextLeftMargin()
	{
		return getScreenNumericalValue(this.getCellElement(mxVsdxConstants.LEFT_MARGIN), 0);
	}

	/**
	 * Returns the right margin of text in pixels.
	 * @return Numerical value of the RightMargin element.
	 */
	public double getTextRightMargin()
	{
		return getScreenNumericalValue(this.getCellElement(mxVsdxConstants.RIGHT_MARGIN), 0);
	}

	/**
	 * Returns the style of one text fragment.
	 * @param charIX IX attribute of Char element
	 * @return String value of the Style element.
	 */
	public String getTextStyle(String index)
	{
		Element styleElem = getCellElement(mxVsdxConstants.STYLE, index, mxVsdxConstants.CHARACTER);
		return getValue(styleElem, "");
	}

	/**
	 * Returns the font of one text fragment
	 * @param charIX IX attribute of Char element
	 * @return Name of the font.
	 */
	public String getTextFont(String index)
	{
		Element fontElem = getCellElement(mxVsdxConstants.FONT, index, mxVsdxConstants.CHARACTER);
		return getValue(fontElem, "");
	}

	/**
	 * Returns the position of one text fragment
	 * @param charIX IX attribute of Char element
	 * @return Integer value of the Pos element.
	 */
	public String getTextPos(String index)
	{
		Element posElem = getCellElement(mxVsdxConstants.POS, index, mxVsdxConstants.CHARACTER);
		return getValue(posElem, "");
	}

	/**
	 * Checks if one text fragment is Strikethru
	 * @param charIX IX attribute of Char element
	 * @return Returns <code>true</code> if one text fragment is Strikethru
	 */
	public boolean getTextStrike(String index)
	{
		Element strikeElem = getCellElement(mxVsdxConstants.STRIKETHRU, index, mxVsdxConstants.CHARACTER);
		return getValue(strikeElem, "").equals("1");
	}

	/**
	 * Returns the case property of one text fragment
	 * @param charIX IX attribute of Char element
	 * @return Integer value of the Case element
	 */
	public String getTextCase(String index)
	{
		Element caseElem = getCellElement(mxVsdxConstants.CASE, index, mxVsdxConstants.CHARACTER);
		return getValue(caseElem, "");
	}

	/**
	 * Returns the horizontal align property of a paragraph
	 * @param index IX attribute of Para element
	 * @param html whether to return the html values or mxGraph values
	 * @return String value of the HorizontalAlign element.
	 */
	public String getHorizontalAlign(String index, boolean html)
	{
		String ret = "center";
		Element horAlign = getCellElement(mxVsdxConstants.HORIZONTAL_ALIGN, index, mxVsdxConstants.PARAGRAPH);
		String align = getValue(horAlign, "");
		
		switch (align)
		{
			case "0":
				ret = html ? "left" : mxConstants.ALIGN_LEFT;
				break;
			case "2":
				ret = html ? "right" : mxConstants.ALIGN_RIGHT;
				break;
			case "3":
			case "4":
				ret = html ? "justify" : mxConstants.ALIGN_CENTER;
				break;
			default:
				ret = html ? "center" : mxConstants.ALIGN_CENTER;
		}
		
		return ret;
	}

	/**
	 * Returns the first indent of one paragraph in pixels.
	 * @param paraIX IX attribute of Para element
	 * @return String representation of the numerical value of the IndentFirst element.
	 */
	public String getIndentFirst(String index)
	{
		Element indentFirstElem = getCellElement(mxVsdxConstants.INDENT_FIRST, index, mxVsdxConstants.PARAGRAPH);
		return String.valueOf(getScreenNumericalValue(indentFirstElem, 0));
	}

	/**
	 * Returns the indent to left of one paragraph
	 * @param paraIX IX attribute of Para element
	 * @return String representation of the numerical value of the IndentLeft element.
	 */
	public String getIndentLeft(String index)
	{
		Element indentLeftElem = getCellElement(mxVsdxConstants.INDENT_LEFT, index, mxVsdxConstants.PARAGRAPH);
		return String.valueOf(getScreenNumericalValue(indentLeftElem, 0));
	}

	/**
	 * Returns the indent to right of one paragraph
	 * @param paraIX IX attribute of Para element
	 * @return String representation of the numerical value of the IndentRight element.
	 */
	public String getIndentRight(String index)
	{
		Element indentRightElem = getCellElement(mxVsdxConstants.INDENT_RIGHT, index, mxVsdxConstants.PARAGRAPH);
		return String.valueOf(getScreenNumericalValue(indentRightElem, 0));
	}

	/**
	 * Returns the space before one paragraph.
	 * @param paraIX IX attribute of Para element
	 * @return String representation of the numerical value of the SpBefore element.
	 */
	public String getSpBefore(String index)
	{
		Element spBeforeElem = getCellElement(mxVsdxConstants.SPACE_BEFORE, index, mxVsdxConstants.PARAGRAPH);
		return String.valueOf(getScreenNumericalValue(spBeforeElem, 0));
	}

	/**
	 * Returns the space after one paragraph
	 * @param paraIX IX attribute of Para element
	 * @return String representation of the numerical value of the SpAfter element.
	 */
	public String getSpAfter(String index)
	{
		Element spAfterElem = getCellElement(mxVsdxConstants.SPACE_AFTER, index, mxVsdxConstants.PARAGRAPH);
		return String.valueOf(getScreenNumericalValue(spAfterElem, 0));
	}

	/**
	 * Returns the space between lines in one paragraph.
	 * @param paraIX IX attribute of Para element.
	 * @return Double representation of the value of the SpLine element.
	 */
	public double getSpLine(String index)
	{
		Element spLineElem = getCellElement(mxVsdxConstants.SPACE_LINE, index, mxVsdxConstants.PARAGRAPH);
		String val = getValue(spLineElem, "");

		if (!val.equals(""))
		{
			return Double.parseDouble(val);
		}

		return 0;
	}

	/**
	 * Returns the flags of one paragraph.
	 * @param paraIX IX attribute of Para element.
	 * @return String value of the Flags element.
	 */
	public String getFlags(String index)
	{
		Element flagsElem = getCellElement(mxVsdxConstants.FLAGS, index, mxVsdxConstants.PARAGRAPH);
		return getValue(flagsElem, "0");
	}

	/**
	 * Returns the space between characters in one text fragment.
	 * @param paraIX IX attribute of Para element.
	 * @return String representation of the numerical value of the Letterspace element.
	 */
	public String getLetterSpace(String index)
	{
		Element letterSpaceElem = getCellElement(mxVsdxConstants.LETTER_SPACE, index, mxVsdxConstants.PARAGRAPH);
		return String.valueOf(getScreenNumericalValue(letterSpaceElem, 0));
	}

	/**
	 * Returns the bullet element value.
	 * @param paraIX IX attribute of Para element.
	 * @return String value of the Bullet element.
	 */
	public String getBullet(String index)
	{
		Element bulletElem = getCellElement(mxVsdxConstants.BULLET, index, mxVsdxConstants.PARAGRAPH);
		return getValue(bulletElem, "0");
	}
	
	public Element getShape() {
		return shape;
	}

	public void setShape(Element shape) {
		this.shape = shape;
	}
}
