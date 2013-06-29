package cruxic.stdgui.layout.model;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import static cruxic.stdgui.layout.Axis.*;

/**

 */
public class ModelFromXML extends DefaultHandler
{
	private LinkedList<Box> box_stack;
	/**a slighly hacky way to keep track of the next alignment group to assign.
	 The top index is box_stack.size()*/
	private int[] alignmentGroupStack;

	private StringBuilder last_chars;

	public ModelFromXML()
	{
		last_chars = new StringBuilder();
		box_stack = new LinkedList<Box>();
		alignmentGroupStack = new int[32];
	}

	private int getSnapsAttr(Attributes attributes)
	{
		return getSnapsAttr(attributes, "snap");
	}

	private int getSnapsAttr(Attributes attributes, String attrName)
	{
		int idx = attributes.getIndex(attrName);
		if (idx != -1)
			return Snaps.fromChars(attributes.getValue(idx));
		else
			return Snaps.Snap_None;
	}



	private boolean getBoolAttr(Attributes attributes, String attrName, boolean defaultVal)
	{
		int idx = attributes.getIndex(attrName);
		if (idx != -1)
		{
			String val = attributes.getValue(idx);
			return val.equalsIgnoreCase("true");
		}
		else
			return defaultVal;
	}

	private boolean hasAttr(Attributes attrs, String attrName)
	{
		return attrs.getIndex(attrName) != -1;
	}


	private Padding parsePadAttr(Attributes attributes, String attrName)
	{
		int idx = attributes.getIndex(attrName);
		if (idx != -1)
		{
			String pad = attributes.getValue(idx).trim();

			int[] sides = new int[4];

			//lone integer?
			if (Pattern.matches("\\d+", pad))
			{
				int p = Integer.parseInt(pad);
				sides[0] = p;
				sides[1] = p;
				sides[2] = p;
				sides[3] = p;
			}
			else
			{
				int i = 0;
				for (char c: new char[]{'L', 'R', 'T', 'B'})
				{
					Pattern pat = Pattern.compile(c + "(\\d+)");
					Matcher m = pat.matcher(pad);
					if (m.find())
					{
						sides[i] = Integer.parseInt(m.group(1));
					}

					i++;
				}
			}

			return new Padding(sides[0], sides[1], sides[2], sides[3]);
		}
		else
			return Padding.Padding_Zero;
	}

	private void check_attrs(Attributes attrs, String... allowedNames)
		throws SAXException
	{
		int nAttrs = attrs.getLength();
		for (int i = 0; i < nAttrs; i++)
		{
			String attr = attrs.getQName(i);

			//ToDo: avoid linear search
			boolean found = false;
			for (String allowed: allowedNames)
			{
				if (attr.equals(allowed))
				{
					found = true;
					break;
				}
			}

			if (!found)
				throw new SAXException("Illegal attribute: \"" + attr + "\"");
		}
	}

	public void startElement(String uri, String localName, String qName,
	  Attributes attributes) throws SAXException
	{
		if (qName.equals("layout"))
		{
			//throw if illegal attribute names are used
			check_attrs(attributes, "pad", "auto-margins");

			if (!box_stack.isEmpty())
				throw new SAXException("Unexpected <layout> node");

			Padding pad = parsePadAttr(attributes, "pad");

			Box box = new Box(null, AXIS_V, Snaps.Snap_All, pad);
			box.enable_auto_margins = getBoolAttr(attributes, "auto-margins", true);

			//All layouts start with an implicit column which fills the container
			box_stack.add(box);
			alignmentGroupStack[box_stack.size()] = 0;
		}
		else if (qName.equals("col"))
		{
			//throw if illegal attribute names are used
			check_attrs(attributes, "snap", "pad", "auto-margins", "child_align_dir");

			int snaps = getSnapsAttr(attributes);
			Padding pad = parsePadAttr(attributes, "pad");
			//Margins margin = getMarginAttr(attributes);  margins don't make sense on a rows or columns, right?  It's the same as padding.

			//top or bottom?
			if ((snaps & Snaps.Snap_TB) > 0)
				System.err.println("HINT: you specifed vertial snaps on a column.  This has no effect - all columns snap Top and Bottom implicitly.");

			//important: columns implicitly span vertically
			snaps |= Snaps.Snap_TB;

			//Add a new box as a child of the current box
			Box parent = box_stack.getLast();
			Box box = new Box(parent, AXIS_V, snaps, pad);
			box.alignmentGroup = alignmentGroupStack[box_stack.size()];
			box.child_align_dir = getSnapsAttr(attributes, "child_align_dir");

			if (hasAttr(attributes, "auto-margins"))
				box.enable_auto_margins = getBoolAttr(attributes, "auto-margins", true);
			else
				box.enable_auto_margins = parent.enable_auto_margins;

			parent.add(box);

			//Push the new box onto the stack
			box_stack.add(box);
			alignmentGroupStack[box_stack.size()] = 0;
		}
		else if (qName.equals("row"))
		{
			//throw if illegal attribute names are used
			check_attrs(attributes, "snap", "pad", "auto-margins", "child_align_dir");

			int snaps = getSnapsAttr(attributes);
			Padding pad = parsePadAttr(attributes, "pad");
			//Margins margin = getMarginAttr(attributes);  margins don't make sense on a rows or columns, right?  It's the same as padding.

			//left or right?
			if ((snaps & Snaps.Snap_LR) > 0)
				System.err.println("HINT: you specifed horizonal snaps on a row.  This has no effect - all rows snap Left and Right implicitly.");

			//important: rows implicitly span left to right
			snaps |= Snaps.Snap_LR;

			//Add a new box as a child of the current box
			Box parent = box_stack.getLast();
			Box box = new Box(parent, AXIS_H, snaps, pad);
			box.alignmentGroup = alignmentGroupStack[box_stack.size()];
			box.child_align_dir = getSnapsAttr(attributes, "child_align_dir");

			if (hasAttr(attributes, "auto-margins"))
				box.enable_auto_margins = getBoolAttr(attributes, "auto-margins", true);
			else
				box.enable_auto_margins = parent.enable_auto_margins;

			parent.add(box);

			//Push the new box onto the stack
			box_stack.add(box);
			alignmentGroupStack[box_stack.size()] = 0;
		}
		else if (qName.equals("button")
			|| qName.equals("label")
			|| qName.equals("entry"))
		{
			//throw if illegal attribute names are used
			check_attrs(attributes, "snap", "margin");

			last_chars.setLength(0);
			int snaps = getSnapsAttr(attributes);
			Padding margin = parsePadAttr(attributes, "margin");

			//Add a new box to hold the component
			Box parent = box_stack.getLast();
			Box box = new Box(parent, AXIS_NA, snaps, margin);
			box.alignmentGroup = alignmentGroupStack[box_stack.size()];

			box.enable_auto_margins = parent.enable_auto_margins;

			parent.add(box);

			//Push the new box onto the stack
			box_stack.add(box);
			alignmentGroupStack[box_stack.size()] = 0;
		}
		else if (qName.equals("align"))
		{
			alignmentGroupStack[box_stack.size()]++;
		}
		else
			throw new SAXException("Unrecognized element: " + qName);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if (qName.equals("layout"))
		{
			//Don't pop lest we lose our handle to the root of the Box tree
		}
		else if (qName.equals("col"))
		{
			//Pop the box
			box_stack.removeLast();
		}
		else if (qName.equals("row"))
		{
			//Pop the box
			box_stack.removeLast();
		}
		else if (qName.equals("button"))
		{
			SnapComponent cmp = new SnapComponent(SnapComponent.Type.BUTTON);
			cmp.text = last_chars.toString().trim();
			if (cmp.text.length() == 0)
				throw new SAXException("<button> missing text content");

			box_stack.getLast().component = cmp;

			//Pop the component box
			box_stack.removeLast();
		}
		else if (qName.equals("label"))
		{
			SnapComponent cmp = new SnapComponent(SnapComponent.Type.LABEL);
			cmp.text = last_chars.toString().trim();
			if (cmp.text.length() == 0)
				throw new SAXException("<label> missing text content");

			box_stack.getLast().component = cmp;

			//Pop the component box
			box_stack.removeLast();
		}
		else if (qName.equals("entry"))
		{
			SnapComponent cmp = new SnapComponent(SnapComponent.Type.ENTRY);
			cmp.text = last_chars.toString().trim();
			if (cmp.text.length() == 0)
				throw new SAXException("<entry> missing text content");

			box_stack.getLast().component = cmp;

			//Pop the component box
			box_stack.removeLast();
		}
		else if (qName.equals("align"))
		{
			
		}
		else
			throw new SAXException("Unrecognized element: " + qName);

	}


	public void characters(char ch[], int start, int length) throws SAXException
	{
		last_chars.append(ch, start, length);
	}


	public Box parseLayoutFromXMLFile(File file)
	  throws IOException
	{
		try
		{
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			saxParser.parse(file, this);

			Box root = box_stack.getFirst();

			root.applyAutoMargins();

			return root;
		}
		catch (ParserConfigurationException e)
		{
			throw new IOException(e);
		}
		catch (SAXException e)
		{
			throw new IOException(e);
		}
	}
}
