package cruxic.stdgui.layout;

import cruxic.stdgui.layout.model.*;

import java.util.List;

/**
	The job of the Sizer is to decide if we have enough room to use the preferred size
	for a given child.
 */
class Sizer
{
	private static final Sizer NO_SHRINK = new Sizer(false, 1.0f);

	public final boolean needsShrink;
	private final float proportion;

	public static Sizer forNode(int axis, final int size, Padding origPad, List<SnapGroup> child_groups)
	{
		int sizeOfNonSpanners = 0;
		for (SnapGroup grp: child_groups)
		{
			if (grp.dir != Snaps.SNAP_SPAN)
			{
				for (LayoutNode child: grp)
					sizeOfNonSpanners += child.preferredSize[axis];
			}
		}

		//Include leading and trailing padding
		sizeOfNonSpanners += origPad.leading_and_trailing(axis);

		int roomForNonSpanners = size;

		if (sizeOfNonSpanners > roomForNonSpanners)
			return new Sizer(true, roomForNonSpanners / (float)sizeOfNonSpanners);
		else
			return NO_SHRINK;
	}

	private Sizer(boolean needsShrink, float proportion)
	{
		this.needsShrink = needsShrink;
		this.proportion = proportion;
	}

	public static Sizer forOneWithPadding(int axis, int[] preferredSize, Padding padding, int[] availableSize)
	{
		int desiredSize = preferredSize[axis] + padding.leading_and_trailing(axis);

		//need to shrink?
		if (desiredSize > availableSize[axis])
			return new Sizer(true, availableSize[axis] / (float)desiredSize);
		else
			return NO_SHRINK;
	}

	public int size(int preferredSize)
	{
		if (needsShrink)
		{
			return (int)(preferredSize * proportion);  //cast does Math.floor() for me
		}
		else
		{
			return preferredSize;
		}
	}
}
