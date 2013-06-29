package cruxic.stdgui.layout;

//IDEA: in gui programming, a lot of coding time is spent synchronizing the state of the GUI widgets with your program model.
//A much easier paradigm is rendering.  You "render" your program model to the screen using the GUI toolkit.  Any change in
//  the model is thus implicitly shown on the screen.  THAT is compelling!  Since any change causes the program to regenerate
//  it's entire GUI state the trick is for stdgui to quickly determine what changed and what has not without heavy queries to the
//underlying GUI toolkit.  And remember, it's a bidirectional connection...
//
// Note: don't just think in terms of input data changing.  ANYTHING in the UI might change as a result of
// program state, such as the layout or visibility of controls.

//Refactor 1: rename <col> and <row> to <vbox> and <hbox>.  Remove related code comments and terminology
//Refactor 2: require explicit <hbox> or <vbox> as root container
//Refactor 3: I think the code assumes in some places that the layout axis flips at each level of the tree.  The user is not constrained in this way


import cruxic.stdgui.layout.model.*;
import static cruxic.stdgui.layout.Axis.*;
import static cruxic.stdgui.layout.model.Snaps.SNAP_BEG;
import static cruxic.stdgui.layout.model.Snaps.SNAP_END;
import static cruxic.stdgui.layout.model.Snaps.SNAP_CENTER;
import static cruxic.stdgui.layout.model.Snaps.SNAP_SPAN;


import java.util.*;

/**
	The real layout logic happens here.  A LayoutNode is a tree
 	structure where every node represents either a column, row, or component.
 */
public class LayoutNode
{
	/**components are not allowed to become any smaller than this*/
	static final int MIN_SIZE = 2;

	static final List<SnapGroup>[] ZERO_GROUPS = new List[0];

	/**The axis on which the children of this Box are stacked.
	 Columns stack their children vertically, Rows stack their
	 children horizontally.  The axis of a leaf node (component)
	 is undefined.
	 */
	final int axis;

	final int snaps;

	/**only boxes have padding (not components)*/
	final Padding pad;

	/**only components (leaves) have margins (not boxes)*/
	Padding margins;

	/**Child components are grouped according to how they snap.
	 If alignment is being used they are grouped again into "cells".
	 Each cell is a logically a member of the greater column or row being aligned.
	 If alignment is not being used there will be only one cell.
	 child_groups[0] is the first column, child_groups[1] is the second etc.
	 */
	List<SnapGroup>[] child_groups;

	/**If alignment is being used this tells meta-data each column or row being
	 aligned such as the width of the column.  Index zero is the first column.

	 All siblings share the same reference to the array.
	 */
	AlignmentGroup[] alignmentGroups;

	/**Leaf nodes carry a component*/
	ComponentInterface component;

	/**The width and height assigned to this node as a result of the layout algorithm.
	 This value, along with position is the ultimate goal of this entire class.
	 The value is undefined until layout() is called.
	 */
	int[] size;

	/**The x or y coordinate assigned to this node as a result of the layout algorithm.
	 This value, along with size is the ultimate goal of this entire class.
	 The value is undefined until layout() is called.*/
	int[] position;

	/**The size which will make this node look it's best.  If there is enough room,
	 all none spanner nodes will be sized to their preferred size.

	 This value is only meaningful after computePreferredSizes() is called.
	 */
	public int[] preferredSize;

	//cached result of ComponentWrapper.getBaseline()
	private int componentBaseline;

	/**Only meaningful if 'spans' is true.
	 The proportion of space given to this node.  1.0 means it gets all space,
	 0.0 means it gets none.  Defaults to equal amount among all spanners on the axis (1.0/numSpanners)*/
	//float spanElasticity;

	public LayoutNode(Box box, LayoutNode parent, IdentityHashMap<SnapComponent, ? extends ComponentInterface> componentMap)
	{
		this.snaps = box.snaps;

		this.size = new int[] {-1,-1};
		this.position = new int[] {-1,-1};
		this.preferredSize = new int[] {-1,-1};

		//Leaf (component)?
		if (box.isEmpty())
		{
			//Leaves with padding really means margins
			margins = box.pad;
			pad = null; //could be Padding.Padding_Zero but I want to know if code ever attempts to read it

			this.axis = AXIS_NA;

			component = componentMap.get(box.component);
			if (component == null)
				throw new IllegalStateException("Unable to resolve component");

			child_groups = ZERO_GROUPS;
		}
		//Box
		else
		{
			this.margins = null;  //could be Padding.Padding_Zero but I want to know if code ever attempts to read it
			this.pad = box.pad;
			this.axis = box.pack_axis;

			this.child_groups = new List[box.countAlignmentGroups()];

			List<LayoutNode> ungrouped = new ArrayList<LayoutNode>(box.size());

			//construct an LayoutNode for each child box
			int curGroup = Integer.MIN_VALUE;
			for (Box child_box: box)
			{
				if (child_box.alignmentGroup != curGroup)
				{
					if (!ungrouped.isEmpty())
					{
						//Group the children depending upon their snapDir
						assert curGroup >= 0;
						child_groups[curGroup] = groupChildren(ungrouped);
						ungrouped.clear();
					}

					curGroup = child_box.alignmentGroup;
				}

				ungrouped.add(new LayoutNode(child_box, this, componentMap));
			}

			//pickup the last group
			assert child_groups[curGroup] == null;
			assert !ungrouped.isEmpty();
			child_groups[curGroup] = groupChildren(ungrouped);


			//
			//Setup shared AlignmentGroup[] for children
			//

			int maxNumAlignmentGroups = 0;
			List<LayoutNode> children = getChildren();
			for (LayoutNode child: children)
			{
				if (child.child_groups.length > maxNumAlignmentGroups)
					maxNumAlignmentGroups = child.child_groups.length;
			}

			if (maxNumAlignmentGroups > 1)
			{
				AlignmentGroup[] ags = new AlignmentGroup[maxNumAlignmentGroups];
				final int dir = Snaps.makeDir(box.child_align_dir, opposite_axis(axis));
				for (int i = 0; i < ags.length; i++)
					ags[i] = new AlignmentGroup(dir);

				for (LayoutNode child: children)
					child.alignmentGroups = ags;
			}
		}
	}

	public static LayoutNode constructRootNode(Box rootBox, IdentityHashMap<SnapComponent, ? extends ComponentInterface> componentMap)
	{
		return new LayoutNode(rootBox, null, componentMap);
	}

	private static int opposite_axis(int axis)
	{
		return axis == AXIS_H ? AXIS_V : AXIS_H;
	}

	static String dir_name(int dir)
	{
		switch (dir)
		{
			case SNAP_BEG:
				return "SNAP_BEG";
			case SNAP_END:
				return "SNAP_END";
			case SNAP_CENTER:
				return "SNAP_CENTER";
			case SNAP_SPAN:
				return "SNAP_SPAN";
			default:
				return "SNAP_?" + Integer.toString(dir);
		}
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder(64);

		if (isLeaf())
		{
			sb.append("Leaf \"");
			sb.append(component.debugString());
			sb.append('\"');
		}
		//Box
		else if (axis == AXIS_V)
		{
			sb.append("Col: ");
		}
		//Row
		else
		{
			sb.append("Row: ");
		}

		sb.append(", ");
		//sb.append(dir_name(snapDir));
		//sb.append(", ");
		sb.append(Snaps.toChars(snaps));

		sb.append(", ");
		sb.append(position[0]);
		sb.append(',');
		sb.append(position[1]);
		sb.append(' ');
		sb.append(size[0]);
		sb.append('x');
		sb.append(size[1]);

		sb.append(", ");
		if (child_groups != null)  //only null during debugging of constructors
			sb.append(getChildren().size());
		else
			sb.append('?');
		sb.append(" children");


		return sb.toString();
	}

	private List<SnapGroup> groupChildren(List<LayoutNode> ungroupedChildren)
	{
		int[] snap_dirs = new int[ungroupedChildren.size()];

		//Compute all snap dirs
		int idx = 0;
		for (LayoutNode child: ungroupedChildren)
			snap_dirs[idx++] = Snaps.makeDir(child.snaps, this.axis);

		//Detect and fix ambiguous cases
		for (idx = 1; idx < ungroupedChildren.size(); idx++)
		{
			//notice: idx starts at one

			//Snapping against a spanner disables the spanner
			if (snap_dirs[idx - 1] == SNAP_END && snap_dirs[idx] == SNAP_SPAN)
			{
				System.err.println("WARNING: spanner disabled because a non-spanner snaps against it.");
				//Disable the spanner
				snap_dirs[idx] = SNAP_END;
			}
			else if (snap_dirs[idx - 1] == SNAP_SPAN && snap_dirs[idx] == SNAP_BEG)
			{
				System.err.println("WARNING: spanner disabled because a non-spanner snaps against it.");
				//Disable the spanner
				snap_dirs[idx - 1] = SNAP_BEG;
			}
		}


		SnapGroup curGroup = null;
		List<SnapGroup> groups = new ArrayList<SnapGroup>(ungroupedChildren.size());

		idx = 0;
		for (LayoutNode child: ungroupedChildren)
		{
			final int prevDir = idx > 0 ? snap_dirs[idx - 1] : -1;  //-1 will match nothing
			final int childDir = snap_dirs[idx];

			//Determine if this child should belong to curGroup
			//  as opposed to starting a new group.
			boolean keepSameGroup;
			switch (childDir)
			{
				case SNAP_BEG:
					keepSameGroup = prevDir == SNAP_BEG
					  || prevDir == SNAP_CENTER
					  || prevDir == SNAP_END;
					break;
				case SNAP_CENTER:
					keepSameGroup = prevDir == SNAP_END;
					break;
				case SNAP_END:
					keepSameGroup = prevDir == SNAP_END;
					break;
				case SNAP_SPAN:
					keepSameGroup = prevDir == SNAP_SPAN;
					break;
				default:
					throw new IllegalArgumentException(Integer.toString(childDir));
			}

			//Change to new group if needed
			if (curGroup == null || !keepSameGroup)
			{
				//Start a new group
				curGroup = new SnapGroup(childDir);
				groups.add(curGroup);
			}

			curGroup.add(child);

			//If we later learn that this group is centered, update the group
			if (childDir == SNAP_CENTER
			  || (childDir == SNAP_BEG && prevDir == SNAP_END))  //opposing inward snaps causes centering
				curGroup.dir = SNAP_CENTER;

			idx++;
		}

		return groups;
	}

	private static void layout_groups(final int position, final int size, Padding origPad, int axis, List<SnapGroup> child_groups)
	{
		//the essential layout algorithm

		/*
			Notes:

			* The height of a non-spanning box is the sum of the heights of all it's rows.
			  (Because it is non-spanning, any child rows which span become shrunk to their preferred size.)

			* The height of a non-spanning row is the max preferred height of the row's children.
			  (Because the row children never overlap vertically.)

			* The size (height or width) of a spanning box or row is defined not by
			   the children but by the parent container.


		*/

		//The job of the Sizer is to decide if we have enough room to use the preferred size
		//for a given child.
		Sizer sizer = Sizer.forNode(axis, size, origPad, child_groups);

		//Compute the edges of the container after padding.
		//  Use sizer as if padding was just another component.
		final int containerStartPos = position + sizer.size(origPad.leading(axis));
		final int containerEndPos = (position + size) - sizer.size(origPad.trailing(axis));

		//These two values will move closer together as we layout the beginning and ending
		//node groups.
		int startPos = containerStartPos;
		int endPos = containerEndPos;

		//
		// Layout beginning nodes first (those which snap left or top).
		// The groupChildren() function put all beginners into one group.
		// The beginning group will never contain spanners and thus they will all get
		// their preferred size (assuming enough room)
		//
		SnapGroup bgrp = child_groups.get(0);
		if (bgrp.dir == SNAP_BEG)
		{
			for (LayoutNode child: bgrp)
			{
				child.position[axis] = startPos;
				child.size[axis] = sizer.size(child.preferredSize[axis]);
				startPos += child.size[axis];
			}
		}
		//otherwise the first group is a spanner - spanners are done very last

		//
		// Layout the ending nodes (those which snap right or bottom)
		// The groupChildren() function puts all enders into one group.
		// The group will never contain spanners and thus should all be set
		// to their preferred size (if enough room)
		SnapGroup egrp = child_groups.get(child_groups.size() - 1);
		if (egrp.dir == SNAP_END)
		{
			//iterate in reverse (do rightmost/bottommost first)
			ListIterator<LayoutNode> itr = egrp.listIterator(egrp.size());
			while (itr.hasPrevious())
			{
				LayoutNode child = itr.previous();

				child.size[axis] = sizer.size(child.preferredSize[axis]);
				endPos -= child.size[axis];

				//prevent negative position when container shrinks very small
				if (endPos < 0)
					endPos = 0;

				child.position[axis] = endPos;
			}
		}

		//
		// Layout middle groups
		//

		//First we need to do the following:
		//  1) count the number of middle groups
		//  2) set the size of each child in a middle group
		//  3) total the size of each child in a middle group
		int midCount = 0;
		int totalMidSize = 0;
		for (SnapGroup mgrp: child_groups)
		{
			if (mgrp.dir == SNAP_CENTER)
			{
				midCount++;

				for (LayoutNode child: mgrp)
				{
					child.size[axis] = sizer.size(child.preferredSize[axis]);
					totalMidSize += child.size[axis];
				}
			}
		}

		if (midCount > 0)
		{
			//Compute the gap between each middle group
			int gap;
			if (!sizer.needsShrink)
			{
				int spaceBetweenEnds = endPos - startPos;
				assert spaceBetweenEnds >= 0;  //ToDo: will be negative if beg and end groups exceed container size.  I'll need to handle this eventually
				int totalGap = Math.max(spaceBetweenEnds - totalMidSize, 0);  //prevent negative totalGap
				gap = totalGap / (midCount + 1);
			}
			else
				gap = 0;

			int pos = startPos;
			for (SnapGroup mgrp: child_groups)
			{
				if (mgrp.dir == SNAP_CENTER)
				{
					pos += gap;

					for (LayoutNode child: mgrp)
					{
						child.position[axis] = pos;
						//Note: child.size[axis] was set during the first phase (look above)
						pos += child.size[axis];
					}
				}
			}
		}

		//
		// Spanners are last because they consume the unused space.
		//
		int spanStart = containerStartPos;


		int  nChildGroups = child_groups.size();
		for (int i = 0; i < nChildGroups; i++)
		{
			SnapGroup grp = child_groups.get(i);

			if (grp.dir == SNAP_SPAN)
			{
				//Determine spanStop

				int spanStop;		
				//have a next group?
				//Note: next group should never be a spanner group or else the groups should have been merged by groupChildren()
				if (i + 1 < nChildGroups)
				{
					LayoutNode firstInNextGroup = child_groups.get(i + 1).get(0);
					spanStop = firstInNextGroup.position[axis];
				}
				else  //otherwise go to the container edge
					spanStop = containerEndPos;

				//Divide the space evenly
				int totalSpan = spanStop - spanStart;
				int eachSize = totalSpan / grp.size();
				int remainder = totalSpan - (eachSize * grp.size());

				int pos = spanStart;
				for (LayoutNode child: grp)
				{
					int sz = eachSize;

					//If the division was not equal, abitrarily give the remainder to the first spanner
					if (remainder > 0)
					{
						sz += remainder;
						remainder = 0;
					}

					child.position[axis] = pos;
					child.size[axis] = sz;
					pos += sz;
				}
			}
			//SNAP_BEG, SNAP_CENTER or SNAP_END
			else
			{
				//Update spanStart so it will be ready when we encounter a SNAP_SPAN
				LayoutNode lastNodeInGroup = grp.get(grp.size() - 1);
				spanStart = lastNodeInGroup.position[axis] + lastNodeInGroup.size[axis];
			}
		}
	}

	/*Invoke layout algorithm for every node below this LayoutNode.
	After this call, every node will have an updated size and position
	(both horizontally and vertically)*/
	private void layout()
	{
		//Caller must have set size and position
		assert size[0] >= 0;
		assert size[1] >= 0;
		assert position[0] >= 0;
		assert position[1] >= 0;

		//This function will actually never get called for a leaf, but guard just in case.
		//For leaves, the parent node has already set the size and position
		if (!isLeaf())
		{
			//
			//For columns, set *vertical* size and position of all children
			//For rows, set the *horizontal* size and position of all children
			//
			if (alignmentGroups == null)
			{
				assert child_groups.length == 1;
				layout_groups(position[this.axis], size[this.axis], pad, this.axis, child_groups[0]);
			}
			else
			{
				//If there is not enough room we first shrink each column proprotionally according
				// to the columns preferred width (AlignmentGroup.preferredSize).  Such a policy
				// retains proper alignment across all rows.

				boolean needsShrink = size[axis] < preferredSize[axis];
				float shrinkProportion = 0.0f;
				if (needsShrink)
					shrinkProportion = size[axis] / (float)preferredSize[axis];

				assert child_groups.length <= alignmentGroups.length;
				int align_dir = alignmentGroups[0].dir;  //dir is same for all columns

				final int end = position[axis] + size[axis];

				//Leading and trailing padding only applies to the first and last cells.
				//Padding on the opposite axis still applies to them all
				Padding withoutLTPad = pad.without_leading_and_trailing(axis);

				if (align_dir == Snaps.SNAP_BEG)
				{
					int pos = position[axis];

					//For all but the last group...
					for (int i = 0; i < child_groups.length - 1; i++)
					{
						List<SnapGroup> cell = child_groups[i];
						AlignmentGroup ag = alignmentGroups[i];

						int groupSize = ag.preferredSize;
						if (needsShrink)
							groupSize = (int)(groupSize * shrinkProportion);  //cast does Math.floor() for me

						layout_groups(pos, groupSize,
							i == 0 ? pad : withoutLTPad,
							axis, cell);

						pos += groupSize;
					}

					//Last group gets the remaining
					layout_groups(pos, end - pos, pad, this.axis, child_groups[child_groups.length - 1]);
				}
				else if (align_dir == Snaps.SNAP_END)
				{

					//start at the end
					int pos = end;

					//For each column in reverse (skipping the very first column)
					for (int i = alignmentGroups.length - 1; i > 0; i--)
					{
						int groupSize = alignmentGroups[i].preferredSize;
						if (needsShrink)
							groupSize = (int)(groupSize * shrinkProportion);  //cast does Math.floor() for me

						pos -= groupSize;

						//Have a child group for this column?
						if (i < child_groups.length)
						{
							//The last child group gets all space until the end of the table
							if (i + 1 == child_groups.length)
								layout_groups(pos, end - pos, pad, axis, child_groups[i]);
							else
								layout_groups(pos, groupSize, withoutLTPad, axis, child_groups[i]);
						}
					}

					//First column gets the remaining space
					if (child_groups.length == 1)  //if only one group, it gets it all!
						layout_groups(position[axis], end - position[axis], pad, axis, child_groups[0]);
					else
						layout_groups(position[axis], pos - position[axis], pad, axis, child_groups[0]);
				}
				else if (align_dir == Snaps.SNAP_CENTER)
				{
					//The semantics of align_dir CENTER is thus:
					// the preferred width of the table will be centered in the available
					// space.  However, the first and last columns are allowed to use the excess
					// space on either side of the table (span into it)

					//If only one group it just spans the whole thing
					if (child_groups.length == 1)
					{
						layout_groups(position[axis], size[axis], pad, axis, child_groups[0]);
					}
					else
					{
						int tableSize = preferredSize[axis];
						if (needsShrink)
							tableSize = size[axis];

						int leadingEdge = (size[axis] / 2) - (tableSize / 2);

						int groupSize = alignmentGroups[0].preferredSize;
						if (needsShrink)
							groupSize = (int)(groupSize * shrinkProportion);  //cast does Math.floor() for me

						int pos = leadingEdge + groupSize;  //the first column wall which is not on the edge of the table
						layout_groups(position[axis], pos, pad, axis, child_groups[0]);

						for (int i = 1; i < child_groups.length - 1; i++)
						{
							groupSize = alignmentGroups[i].preferredSize;
							if (needsShrink)
								groupSize = (int)(groupSize * shrinkProportion);  //cast does Math.floor() for me

							layout_groups(pos, groupSize, withoutLTPad, axis, child_groups[i]);

							pos += groupSize;
						}

						//last group gets the remaining
						layout_groups(pos, (position[axis] + size[axis]) - pos, pad, axis, child_groups[child_groups.length - 1]);
					}
				}
				//align_dir == Snaps.SNAP_SPAN
				else
				{
					//child_align_dir="LR" means the table spans the entire width and all columns
					//share the width proportionally to their preferred size

					int pos = position[axis];

					float scale = size[axis] / (float)preferredSize[axis];

					//For all but the last
					int i;
					for (i = 0; i < child_groups.length - 1; i++)
					{
						int groupSize = (int)(alignmentGroups[i].preferredSize * scale);

						layout_groups(pos, groupSize, i == 0 ? pad : withoutLTPad, axis, child_groups[i]);

						pos += groupSize;
					}

					//The last one all remaining size (because floating point rounding likely put us off a few pixels)
					layout_groups(pos, end - pos, pad, axis, child_groups[i]);
				}
			}

			//
			// Now set the size and position of the opposite axis for all children
			//
			//Each child has 4 possibilities:
			//  1) Snap left   (or top for columns)
			//  2) Snap right  (or bottom for columns)
			//  3) Span left and right (or top and bottom for columns)
			//  4) Center

			final int opp_axis = axis == AXIS_H ? AXIS_V : AXIS_H;

			SnapGroup the_group = new SnapGroup(-1);
			ArrayList<SnapGroup> one_group = new ArrayList<SnapGroup>(1);
			one_group.add(the_group);


			for (LayoutNode child: getChildren())
			{
				//convert Top or Left to SNAP_BEG, Bottom or Right to SNAP_END etc.
				the_group.dir = Snaps.makeDir(child.snaps, opp_axis);
				the_group.clear();
				the_group.add(child);

				layout_groups(position[opp_axis], size[opp_axis], pad, opp_axis, one_group);
			}

			//At this point, the horizontal and vertical size and position have been
			//  set for all direct children of this node.

			//
			//Make the recursive call for each child
			//
			for (LayoutNode child: getChildren())
				child.layout();
		}
	}

	/**Invoke the layout algorithm*/
	public void setRect(int x, int y, int width, int height)
	{
		position[0] = x;
		position[1] = y;
		size[0] = Math.max(width, MIN_SIZE);  //never smaller than MIN_SIZE
		size[1] = Math.max(height, MIN_SIZE);

		//System.out.printf("%d,%d\t%dx%d\n", x, y, width, height);

		//ToDo: guarantee that computePreferredSizes has happened

		layout();
	}

	/**
	Baseline strategy:

	remember:
	 1) controls with the same baseline have same padding
	 2)

	 LayoutNode.preferredSize is the same for all in a baseline alignment group:
	 the maximum preferredSize of the group.  It includes margins as normal.
	 Thus when margins are subtracted, the value is the true preferred size of the tallest
	 control.

	 IDEA: why not just increase the top margin on all controls to reflect the Y offset needed
	 to align their baselines with the tallest control in the group.  The bottom margin would
	 merely be the remainder necessary to make this hold true:

		 truePreferredSize = preferredSize - (T + B)

	 This would make it seemlessly flow through placeComponentInMargins() and also not require
	 any extra memory.
	 */

	private static void placeComponentInMargins(int position, int size, int preferredSize,
		int leadingMargin, int trailingMargin, int[] resultPositionAndSize)
	{
		//Remember: preferredSize for a leaf already includes the margins

		int shortage = preferredSize - size;

		//Need to shrink?
		if (shortage > 0)
		{
			int bothMargins = leadingMargin + trailingMargin;

			//to shrink away the margins first when possible
			if (shortage < bothMargins)
			{
				//Reduce the trailing margin to zero, and then take the remainder from the leading.
				//I'm not sure if I like this bettern than proportional shrinking or not
				trailingMargin -= shortage;
				if (trailingMargin < 0)
				{
					leadingMargin -= (-trailingMargin);
					trailingMargin = 0;
				}

				//	//Take a proportional amount from each
				//	int truePreferredSize = preferredSize - bothMargins;
				//	float proportion = (size - truePreferredSize) / (float)bothMargins;
				//
				//	leadingMargin = (int)(proportion * leadingMargin);
				//	trailingMargin = (int)(proportion * trailingMargin);
				//
				//	//Optimization?: floating point calculations could be avoided when one margin is zero.  Probably not worth the code bloat.
			}
			//Otherwise eliminate the margins entirely
			else
			{
				leadingMargin = 0;
				trailingMargin = 0;
			}
		}

		//Position
		resultPositionAndSize[0] = position + leadingMargin;
		//Size
		resultPositionAndSize[1] = size - (leadingMargin + trailingMargin);
	}

	public void applyLayout()
	{
		if (isLeaf())
		{
			//layout() must have happened first!
			assert size[0] >= 0;
			assert size[1] >= 0;
			assert position[0] >= 0;
			assert position[1] >= 0;

			int x, y, w, h;
			int[] tuple = new int[2];

			//Horizontal placement
			placeComponentInMargins(position[AXIS_H], size[AXIS_H], preferredSize[AXIS_H],
				margins.L, margins.R, tuple);
			x = tuple[0];
			w = tuple[1];

			//Vertical placement
			placeComponentInMargins(position[AXIS_V], size[AXIS_V], preferredSize[AXIS_V],
				margins.T, margins.B, tuple);
			y = tuple[0];
			h = tuple[1];

			//Prevent insane coordinates that might choke the native GUI toolkit
			if (x < 0)
				x = 0;
			if (y < 0)
				y = 0;
			if (w < MIN_SIZE)
				w = MIN_SIZE;
			if (h < MIN_SIZE)
				h = MIN_SIZE;

			component.setPositionAndSize(x, y, w, h);
/*
			//using baseline alignment
			if (baselineYOffset != 0)
			{
				//Prevent insane coordinates that might choke the native GUI toolkit
				component.setPositionAndSize(
					Math.max(position[AXIS_H], 0),
					Math.max(position[AXIS_V] + baselineYOffset, 0),
					Math.max(size[AXIS_H], MIN_SIZE),
					//use preferred component height unless shrinking was necessary
					Math.max(size[AXIS_V] >= baselineYSize ? baselineYSize : size[AXIS_V], MIN_SIZE));
			}
			else
			{
				//Prevent insane coordinates that might choke the native GUI toolkit
				component.setPositionAndSize(
					Math.max(position[AXIS_H], 0),
					Math.max(position[AXIS_V], 0),
					Math.max(size[AXIS_H], MIN_SIZE),
					Math.max(size[AXIS_V], MIN_SIZE));
			}
*/
		}
		else
		{
			for (LayoutNode child: getChildren())
			{
				child.applyLayout();
			}
		}
	}

	public List<LayoutNode> getChildren()
	{
		List<LayoutNode> all = new ArrayList<LayoutNode>(32);
		for (List<SnapGroup> cell: child_groups)
		{
			for (SnapGroup snapGrp: cell)
				all.addAll(snapGrp);
		}
		return all;
	}

	private boolean isLeaf()
	{
		return component != null;
	}

	/**this function assigns a preferredSize to all nodes.  Nodes which are not spanners
	 will always be set to their preferred size if there is enough room to do so.*/
	public void computePreferredSizes()
	{
		if (isLeaf())
		{
			//ToDo: we only need to query the component once! Not upon each window resize 

			int[] sz = component.getPreferredSize();

			//cache the baseline value using it's preferred size
			componentBaseline = component.getBaseline(sz);

			//Padding on components is really a "margin" (since components don't have children to pad)
			//However, we will pretend it does have children here and just add the padding to
			//preferedSize and let applyLayout() do the rest.
			preferredSize[AXIS_H] = sz[AXIS_H] + margins.leading_and_trailing(AXIS_H);
			preferredSize[AXIS_V] = sz[AXIS_V] + margins.leading_and_trailing(AXIS_V);
		}
		else
		{
			//first compute for all children (depth first)
			List<LayoutNode> children = getChildren();
			for (LayoutNode node: children)
				node.computePreferredSizes();

			//Rows are subject to baseline alignment
			if (axis == AXIS_H)
				doBaselineAlignment(children);


			//If alignment is being used with the children their preferred size is a collective decision
			calcAlignmentPreferredSizes(children);

			int opp_axis = opposite_axis(axis);
			preferredSize[axis] = 0;
			preferredSize[opp_axis] = 0;

			for (LayoutNode child: children)
			{
				//Sum the stacked sizes of children on the axis which the container packs them
				preferredSize[axis] += child.preferredSize[axis];

				//On the opposite axis the preferred size is simply the largest child
				if (child.preferredSize[opp_axis] > preferredSize[opp_axis])
					preferredSize[opp_axis] = child.preferredSize[opp_axis];
			}

			//Padding increases preferredSize
			preferredSize[axis] += pad.leading_and_trailing(axis);
			preferredSize[opp_axis] += pad.leading_and_trailing(opp_axis);
		}
	}

	private void doBaselineAlignment(List<LayoutNode> children)
	{
		//Place children into groups which will be baseline aligned.
		//Groups are created with these criteria:
		//  * Only consider controls with a meaningful baseline (eg not list boxes)
		//  * No sub-boxes
		//  * No controls which span vertically
		//  * Controls must have the same vertical margins
		//  * Controls must have the same vertical snaps (T, B, or none)


		//List<LayoutNode>
		Set<LayoutNode> already_processed = new HashSet<LayoutNode>(16);

		int group_vmargin = 0;
		int group_vsnaps = 0;
		boolean start_new_group;
		LayoutNode groupMaxHeight;  //the LayoutNode with the maximum height in the group

		List<LayoutNode> curGroup = new ArrayList<LayoutNode>(4);

		while (true)
		{
			start_new_group = true;
			groupMaxHeight = null;
			curGroup.clear();
			System.out.println();

			//todo: I could start at a later index each iteration of the outer loop, right?
			for (LayoutNode child: children)
			{
				boolean isCandidate = child.isLeaf()
					&& child.componentBaseline > 0
					&& (child.snaps & Snaps.Snap_TB) != Snaps.Snap_TB;

				if (!already_processed.contains(child)
					&& isCandidate)
				{
					int vsnaps = child.snaps & Snaps.Snap_TB;
					int vmargin = (child.margins.T << 16) | child.margins.B;   //T in hi word, B in lo word

					if (start_new_group)
					{
						group_vsnaps = vsnaps;
						group_vmargin = vmargin;
						start_new_group = false;
					}

					//Does it belong in the group?
					if (vsnaps == group_vsnaps
						&& vmargin == group_vmargin)
					{
						//System.out.printf("Bgrp: %d\t%d %s\n", vsnaps, vmargin, child);

						curGroup.add(child);

						//find the tallest one
						if (groupMaxHeight == null
							|| child.preferredSize[AXIS_V] > groupMaxHeight.preferredSize[AXIS_V])
							groupMaxHeight = child;
					}
				}
			}

			//Nothing groupable remains?
			if (curGroup.isEmpty())
				break;
			else if (curGroup.size() > 1)
			{
				int tallestBaseline = groupMaxHeight.margins.T + groupMaxHeight.componentBaseline;

				int marginT, marginB, trueHeight;

				for (LayoutNode child: curGroup)
				{
					//Use the top margin to align the child's baseline with the baseline of the tallest
					marginT = tallestBaseline - child.componentBaseline;

					//Get the child's true preferred height by removing the margins
					trueHeight = child.preferredSize[AXIS_V] - child.margins.leading_and_trailing(AXIS_V);

					//Bottom margin is the remainder
					marginB = groupMaxHeight.preferredSize[AXIS_V] - (marginT + trueHeight);

					child.margins = child.margins.withAlteredVertical(marginT, marginB);

					//All children in the alignment group are given the same height
					child.preferredSize[AXIS_V] = groupMaxHeight.preferredSize[AXIS_V];
				}
			}
			//else there's only one in the group - nothing to align it with

			//Group each control at most once
			already_processed.addAll(curGroup);
		}
	}


	private void calcAlignmentPreferredSizes(List<LayoutNode> children)
	{
		final int opp_axis = opposite_axis(axis);

		//If any children utilize alignment then we must change how
		//we compute the size of each child.  Take columnar alignment for
		//example: the width of a row containing columnar alignment depends upon
		// the contents of all columns in all rows.  Specifically, the width of each column
		//is the max width of all nodes in that column in any row.
		//And the width of any given row is the sum of all the column widths.  However, rows
		//are allowed to have fewer columns than sibling rows.  In such cases the last column
		//of a row spans all the remaining space til the end of the row.


		AlignmentGroup[] childAGs = null;
		for (LayoutNode child: children)
		{
			if (child.alignmentGroups != null)
			{
				childAGs = child.alignmentGroups;  //all children share the same reference
				break;
			}
		}

		if (childAGs != null)
		{
			assert childAGs.length > 1;  //not strictly necessary but just a sanity check

			//Prepare to calculate the max
			for (AlignmentGroup ag: childAGs)
				ag.preferredSize = 0;

			for (LayoutNode child: children)
			{
				assert child.alignmentGroups == childAGs;  //all children must share the same reference

				//For each child group
				for (int i = 0; i < child.child_groups.length; i++)
				{
					//Some rows can have more columns than others.  If this row
					//has fewer columns than the entire table then it's last
					//column implicitly spans across all remaining columns in the
					//table and therefore is not constrained to a single columns sizing calculation

					//last column in this particular row but not the last column in the table
					if (i + 1 == child.child_groups.length
						&& i + 1 != child.alignmentGroups.length)
						break;

					AlignmentGroup ag = child.alignmentGroups[i];
					List<SnapGroup> cell = child.child_groups[i];
					assert cell.size() > 0;

					int cellSize = 0;

					//The first group must include leading padding and the last
					//group must include trailing padding
					if (i == 0)
						cellSize += child.pad.leading(opp_axis);
					else if (i + 1 == child.child_groups.length)
						cellSize += child.pad.trailing(opp_axis);

					for (SnapGroup cellGrp: cell)
					{
						for (LayoutNode cellChild: cellGrp)
							cellSize += cellChild.preferredSize[opp_axis];
					}

					//Beats the max?
					if (cellSize > ag.preferredSize)
						ag.preferredSize = cellSize;
				}
			}

			//Give each row the exact same preferred size - the sum of the column widths
			int totalSize = 0;
			for (AlignmentGroup ag: childAGs)
				totalSize += ag.preferredSize;
			for (LayoutNode child: children)
				child.preferredSize[opp_axis] = totalSize;


			//This logic gives each row a different preferred size.  Pointless because all rows span their container anyway
			//for (LayoutNode child: children)
			//{
			//	int totalSize = 0;
			//	for (int i = 0; i < child.child_groups.length; i++)
			//	{
			//		boolean isLast = i + 1 == child.child_groups.length;
			//		AlignmentGroup ag = child.alignmentGroups[i];
			//		List<SnapGroup> cell = child.child_groups[i];
			//
			//
			//		if (isLast)
			//		{
			//			for (SnapGroup cellGrp: cell)
			//			{
			//				for (LayoutNode cellChild: cellGrp)
			//					totalSize += cellChild.preferredSize[opp_axis];
			//			}
			//		}
			//		else
			//		{
			//			totalSize += ag.preferredSize;
			//		}
			//	}
			//
			//	child.preferredSize[opp_axis] = totalSize;
			//	//System.out.printf("PS: %d\n", totalSize);
			//}
		}
	}

}

