package cruxic.stdgui.layout.model;

import static cruxic.stdgui.layout.Axis.AXIS_V;
import static cruxic.stdgui.layout.Axis.AXIS_H;

/**

 */
public class Snaps
{
	public static final int Snap_None = 0;
	public static final int Snap_T = 1;
	public static final int Snap_B = 2;
	public static final int Snap_L = 4;
	public static final int Snap_R = 8;
	public static final int Snap_All = Snap_T | Snap_B | Snap_L | Snap_R;
	public static final int Snap_LR = Snap_L | Snap_R;
	public static final int Snap_TB = Snap_T | Snap_B;

	public static final int SNAP_CENTER = Snap_None;
	/**Only use for Top or Left snaps depending on axis*/
	public static final int SNAP_BEG = Snap_T;  //makeDir() makes some assumptions about this
	/**Only use for Bottom or Right snaps depending on axis*/
	public static final int SNAP_END = Snap_B;  //makeDir() makes some assumptions about this
	public static final int SNAP_SPAN = SNAP_BEG | SNAP_END;

	//Nobody builds this class
	private Snaps()
	{

	}

	public static int fromChars(String chars)
	  throws IllegalArgumentException
	{
		int m = Snap_None;

		//parse snap directions
		for (int i = 0; i < chars.length(); i++)
		{
			char c = chars.charAt(i);
			switch (c)
			{
				case 't':
				case 'T':
					m |= Snap_T;
					break;
				case 'b':
				case 'B':
					m |= Snap_B;
					break;
				case 'l':
				case 'L':
					m |= Snap_L;
					break;
				case 'r':
				case 'R':
					m |= Snap_R;
					break;
				default:
					throw new IllegalArgumentException("Unrecognized snap character: " + c);
			}
		}

		return m;
	}

	public static String toChars(int snaps)
	{
		StringBuilder sb = new StringBuilder(4);
		if ((snaps & Snap_T) > 0)
			sb.append('T');
		if ((snaps & Snap_B) > 0)
			sb.append('B');
		if ((snaps & Snap_L) > 0)
			sb.append('L');
		if ((snaps & Snap_R) > 0)
			sb.append('R');

		return sb.toString();
	}

	/**Convert the snaps into a more abstract "snap direction" on a particular axis.
	 */
	public static int makeDir(int snaps, int axis)
	{
		if (axis == AXIS_V)
			return snaps & Snap_TB;  //works because Snap_T and Snap_B == SNAP_BEG and SNAP_END
		else if (axis == AXIS_H)
			return snaps >> 2;  //Snap_L and Snap_R are same as SNAP_BEG and SNAP_END just shifted up 2 bits
		else
		{
			System.err.println("makeDir: Invalid axis: " + axis);
			return SNAP_CENTER;
		}
	}

//
//	public String toString()
//	{
//		return getChars();
//	}
//
//	public boolean top()
//	{
//		return (mask & TOP) > 0;
//	}
//
//	public boolean bot()
//	{
//		return (mask & BOT) > 0;
//	}
//
//	public boolean left()
//	{
//		return (mask & LEFT) > 0;
//	}
//
//	public boolean right()
//	{
//		return (mask & RIGHT) > 0;
//	}
//
//	public boolean top_and_bot()
//	{
//		return (mask & TB) > 0;
//	}
//
//	public boolean left_and_right()
//	{
//		return (mask & LR) > 0;
//	}
}
