package cruxic.stdgui.swing;

import cruxic.stdgui.layout.*;
import cruxic.stdgui.layout.model.*;

import java.awt.*;
import java.util.IdentityHashMap;

/**

 */
public class SnapLayout implements LayoutManager2
{
	public LayoutNode root;

	public SnapLayout(Box rootBox, IdentityHashMap<SnapComponent, ComponentWrapper> componentMap)
	{
		root = LayoutNode.constructRootNode(rootBox, componentMap);
	}

	public float getLayoutAlignmentX(Container target)
	{
		//??
		return 0.0f;
	}

	public float getLayoutAlignmentY(Container target)
	{
		//??
		return 0.0f;
	}

	public void invalidateLayout(Container target)
	{
	}

	public void addLayoutComponent(Component comp, Object constraints)
	{
	}

	public void addLayoutComponent(String name, Component comp)
	{
	}

	public void removeLayoutComponent(Component comp)
	{
	}


	public Dimension preferredLayoutSize(Container parent)
	{
		root.computePreferredSizes();
		return new Dimension(root.preferredSize[0], root.preferredSize[1]);
	}

	public Dimension minimumLayoutSize(Container parent)
	{
		return new Dimension(1, 1);
	}

	public Dimension maximumLayoutSize(Container target)
	{
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	public void layoutContainer(Container parent)
	{
		//ToDo: account for insets?
		//Insets insets = parent.getInsets();

		Dimension parSize = parent.getSize();

		root.computePreferredSizes();
		root.setRect(0, 0, parSize.width, parSize.height);
		//root.setRect(0, 0, 1, 454);


		root.applyLayout();

//		//Vertical layout
//		root.layout(0, parSize.height);
//
//		//Horizontal layout
//		for (LayoutNode row: root.getChildren())
//			row.layout(0, parSize.width);


	}


}
