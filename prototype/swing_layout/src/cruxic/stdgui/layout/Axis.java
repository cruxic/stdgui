package cruxic.stdgui.layout;

/**

 */
public class Axis
{
	public static final int AXIS_H = 0;
	public static final int AXIS_V = 1;
	public static final int AXIS_NA = -1;

	public static String getName(int axis)
	{
		switch (axis)
		{
			case AXIS_H:
				return "AXIS_H";
			case AXIS_V:
				return "AXIS_V";
			case AXIS_NA:
				return "AXIS_NA";
			default:
				return "AXIS_?(" + axis + ")";
		}
	}
}
