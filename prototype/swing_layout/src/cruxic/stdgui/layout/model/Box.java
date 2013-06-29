package cruxic.stdgui.layout.model;

import cruxic.stdgui.layout.Axis;

import java.util.*;

/**

 */
public class Box extends ArrayList<Box>
{
	/**The axis which this Box's children are packed (vertically or horizontally)*/
	public final int pack_axis;

	boolean enable_auto_margins;

	public int snaps;
	public Padding pad;
	public int child_align_dir;

	/**Leaf nodes carry a component*/
	public SnapComponent component;

	//null for the root box
	private Box parent;

	/**Boxes preceeding an &lt;align&gt; tag are placed in the same alignment group.*/
	public int alignmentGroup;

	//public Margins margin;  margins don't make sense on a rows or columns, right?  It's the same as padding.

	public Box(Box parent, int pack_axis, int snaps, Padding pad)
	{
		this.parent = parent;
		this.pack_axis = pack_axis;
		this.snaps = snaps;
		this.pad = pad;
		this.alignmentGroup = 0;
		this.child_align_dir = Snaps.Snap_None;
	}

	public int countAlignmentGroups()
	{
		int count = 0;
		int lastGroup = Integer.MIN_VALUE;
		for (Box child: this)
		{
			if (child.alignmentGroup != lastGroup)
			{
				count++;
				lastGroup = child.alignmentGroup;
			}
		}

		return count;
	}

	public int getMaxAlignmentGroupCountOfChildren()
	{
		int max = Integer.MIN_VALUE;
		for (Box child: this)
		{
			int count = child.countAlignmentGroups();
			if (count > max)
				max = count;
		}

		return max;
	}

	public void debugPrint(String indent)
	{
		System.out.print(indent);

		//Leaf?
		if (isEmpty())
		{
			System.out.print(component.type.name());
			System.out.print(": \"");
			System.out.print(component.text);
			System.out.print('\"');
		}
		else
		{
			if (pack_axis == Axis.AXIS_H)
				System.out.print("Row");
			else
				System.out.print("Column");
		}

		System.out.print(' ');
		System.out.print(Snaps.toChars(snaps));

		System.out.print(", AG:");
		System.out.println(this.alignmentGroup);

		String indent1 = indent + '\t';

		for (Box child: this)
		{
			child.debugPrint(indent1);
		}
	}

	/**Get all Components within this Box and any of it's sub-boxes.*/
	public List<SnapComponent> getComponents()
	{
		List<SnapComponent> all = new ArrayList<SnapComponent>(32);
		_getComponents(all);
		return all;
	}

	private void _getComponents(List<SnapComponent> list)
	{
		for (Box child_box: this)
		{
			if (child_box.isEmpty())
			{
				assert child_box.component != null;
				list.add(child_box.component);
			}
			else
				child_box._getComponents(list);
		}
	}

	public Box getLast()
	{
		if (isEmpty())
			throw new NoSuchElementException("zero rows");
		else
			return get(size() - 1);
	}

	private boolean isLeftMost()
	{
		//no parent?
		if (parent == null)
			return true;
		//parent is a row?
		else if (parent.pack_axis == Axis.AXIS_H)
		{
			//I am first in the row?
			if (parent.get(0) == this)
				return parent.isLeftMost();
			else
				return false;
		}
		//parent is a column
		else
			return parent.isLeftMost();
	}

	private boolean isTopMost()
	{
		//no parent?
		if (parent == null)
			return true;
		//parent is a column?
		else if (parent.pack_axis == Axis.AXIS_V)
		{
			//I am first in the column?
			if (parent.get(0) == this)
				return parent.isTopMost();
			else
				return false;
		}
		//parent is a row
		else
			return parent.isTopMost();
	}

	public void applyAutoMargins()
	{
		//Leaf (component)?
		if (component != null)
		{
			//auto-margins enabled and no explicit margins?
			if (parent.enable_auto_margins && pad == Padding.Padding_Zero)
			{
				int pL = 0, pR = 1, pT = 0, pB = 1;

				//ToDo: the implementation of isTopMost and isLeftMost asks the same questions of all the parents for every
				//component in this container.  Seems like you could optimize that somehow and only ask once... might just be a memory vs cpu tradeoff
				if (isLeftMost())
					pL = 1;
				if (isTopMost())
					pT = 1;

				pad = new Padding(pL, pR, pT, pB);
			}
		}
		else
		{
			for (Box child: this)
				child.applyAutoMargins();
		}
	}
}
