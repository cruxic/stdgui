package cruxic.stdgui.layout;


/**

 */
public interface ComponentInterface
{
	public int[] getPreferredSize();
	public Object getComponent();
	public void setPositionAndSize(int x, int y, int w, int h);

	/**Return the distance from the components top edge down to it's text baseline.
	 Components which have no meaningful baseline return -1.
	 */
	public int getBaseline(int[] preferredSize);

	public String debugString();
}
