package cruxic.stdgui.layout;

import cruxic.stdgui.layout.model.Snaps;

import java.util.List;

/**
For handling the <align> directive.  AlignmentGroup holds
 info about an entire column (or row) such as the preferred width of the column
 and which direction the column snaps (left or right)
 */
class AlignmentGroup
{
	/**For columnar alignment groups this holds the preferred width of the
	 column (which is simply the maximum width from all rows with this column).
	 Same idea for the row-based alignment.*/
	int preferredSize;

	/**
	 Snaps.SNAP_BEG, END, CENTER, or SPAN.
	 from child_align_dir parent node in the XML.  Same for every sibling*/
	final int dir;

	public AlignmentGroup(int align_dir)
	{
		dir = align_dir;
	}

	public static int sumPreferredSize(AlignmentGroup[] ags)
	{
		int sum = 0;
		for (AlignmentGroup ag: ags)
			sum += ag.preferredSize;
		return sum;
	}
}
