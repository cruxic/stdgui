package cruxic.stdgui.layout.model;

import cruxic.stdgui.layout.Axis;

/**

 */
public class Padding
{
	private static final int PIXELS_PER_UNIT = 4;

	public static final int AUTO = -1;

	public static final Padding Padding_Auto = new Padding(AUTO, AUTO, AUTO, AUTO);
	public static final Padding Padding_Zero = new Padding(0, 0, 0, 0);

	public final int L;
	public final int R;
	public final int T;
	public final int B;

	public Padding(int leftUnits, int rightUnits, int topUnits, int bottomUnits)
	{
		this.L = leftUnits * PIXELS_PER_UNIT;
		this.R = rightUnits * PIXELS_PER_UNIT;
		this.T = topUnits * PIXELS_PER_UNIT;
		this.B = bottomUnits * PIXELS_PER_UNIT;
	}

	private Padding(boolean unused, int l, int r, int t, int b)
	{
		this.L = l;
		this.R = r;
		this.T = t;
		this.B = b;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder(16);
		sb.append('L');
		sb.append(L);
		sb.append(',');

		sb.append('R');
		sb.append(R);
		sb.append(',');

		sb.append('T');
		sb.append(T);
		sb.append(',');

		sb.append('B');
		sb.append(B);

		return sb.toString();
	}

	public int leading(int axis)
	{
		if (axis == Axis.AXIS_H)
			return L;
		else
			return T;
	}

	public int trailing(int axis)
	{
		if (axis == Axis.AXIS_H)
			return R;
		else
			return B;
	}

	public int leading_and_trailing(int axis)
	{
		if (axis == Axis.AXIS_H)
			return L + R;
		else
			return T + B;
	}

	//return copy with zero padding on the given axis
	public Padding without_leading_and_trailing(int axis)
	{
		if (axis == Axis.AXIS_H)
			return new Padding(false, 0, 0, T, B);
		else
			return new Padding(false, L, R, 0, 0);
	}

	public Padding withAlteredVertical(int newT, int newB)
	{
		return new Padding(false, L, R, newT, newB);
	}


}
