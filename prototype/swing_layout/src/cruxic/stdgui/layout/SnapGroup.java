package cruxic.stdgui.layout;

import java.util.*;

/**
	A grouping of components nodes which snap together vertically or horizontally.
 */
class SnapGroup extends ArrayList<LayoutNode>
{
	public int dir;

	public SnapGroup(int dir)
	{
		super(4);
		this.dir = dir;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder(64);
		sb.append(LayoutNode.dir_name(dir));
		sb.append(", ");
		sb.append(size());
		sb.append(" nodes: ");
		for (LayoutNode node: this)
		{
			sb.append('{');
			sb.append(node.toString());
			sb.append("} ");
		}

		return sb.toString();
	}
}
