
#include "gtksnaplayout.h"


/*
enum {
  CHILD_PROP_0,
  CHILD_PROP_X,
  CHILD_PROP_Y
};
*/

static void gtk_snaplayout_realize       (GtkWidget        *widget);
static void gtk_snaplayout_size_request  (GtkWidget        *widget,
				     GtkRequisition   *requisition);
static void gtk_snaplayout_size_allocate (GtkWidget        *widget,
				     GtkAllocation    *allocation);
static void gtk_snaplayout_add           (GtkContainer     *container,
				     GtkWidget        *widget);
static void gtk_snaplayout_remove        (GtkContainer     *container,
				     GtkWidget        *widget);
static void gtk_snaplayout_forall        (GtkContainer     *container,
				     gboolean 	       include_internals,
				     GtkCallback       callback,
				     gpointer          callback_data);
static GType gtk_snaplayout_child_type   (GtkContainer     *container);

/*static void gtk_snaplayout_set_child_property (GtkContainer *container,
                                          GtkWidget    *child,
                                          guint         property_id,
                                          const GValue *value,
                                          GParamSpec   *pspec);
static void gtk_snaplayout_get_child_property (GtkContainer *container,
                                          GtkWidget    *child,
                                          guint         property_id,
                                          GValue       *value,
                                          GParamSpec   *pspec);
*/

G_DEFINE_TYPE (GtkSnapLayout, gtk_snaplayout, GTK_TYPE_CONTAINER)

static void
gtk_snaplayout_class_init (GtkSnapLayoutClass *class)
{
  GtkWidgetClass *widget_class;
  GtkContainerClass *container_class;

  widget_class = (GtkWidgetClass*) class;
  container_class = (GtkContainerClass*) class;

  widget_class->realize = gtk_snaplayout_realize;
  widget_class->size_request = gtk_snaplayout_size_request;
  widget_class->size_allocate = gtk_snaplayout_size_allocate;

  container_class->add = gtk_snaplayout_add;
  container_class->remove = gtk_snaplayout_remove;
  container_class->forall = gtk_snaplayout_forall;
  container_class->child_type = gtk_snaplayout_child_type;

/*
  container_class->set_child_property = gtk_snaplayout_set_child_property;
  container_class->get_child_property = gtk_snaplayout_get_child_property;


  gtk_container_class_install_child_property (container_class,
					      CHILD_PROP_X,
					      g_param_spec_int ("x",
                                                                P_("X position"),
                                                                P_("X position of child widget"),
                                                                G_MININT,
                                                                G_MAXINT,
                                                                0,
                                                                GTK_PARAM_READWRITE));

  gtk_container_class_install_child_property (container_class,
					      CHILD_PROP_Y,
					      g_param_spec_int ("y",
                                                                P_("Y position"),
                                                                P_("Y position of child widget"),
                                                                G_MININT,
                                                                G_MAXINT,
                                                                0,
                                                                GTK_PARAM_READWRITE));
*/
}

static GType
gtk_snaplayout_child_type (GtkContainer     *container)
{
  return GTK_TYPE_WIDGET;
}

static void
gtk_snaplayout_init (GtkSnapLayout *snaplayout)
{
	gtk_widget_set_has_window (GTK_WIDGET (snaplayout), FALSE);

	snaplayout->children = NULL;
}

GtkWidget*
gtk_snaplayout_new (void)
{
  return g_object_new (GTK_TYPE_SNAPLAYOUT, NULL);
}

static GtkSnapLayoutChild*
get_child (GtkSnapLayout  *snaplayout,
           GtkWidget *widget)
{
  GList *children;

  children = snaplayout->children;
  while (children)
    {
      GtkSnapLayoutChild *child;

      child = children->data;
      children = children->next;

      if (child->widget == widget)
        return child;
    }

  return NULL;
}

void
gtk_snaplayout_put (GtkSnapLayout       *snaplayout,
               GtkWidget      *widget)
{
  GtkSnapLayoutChild *child_info;

  g_return_if_fail (GTK_IS_SNAPLAYOUT (snaplayout));
  g_return_if_fail (GTK_IS_WIDGET (widget));

  child_info = g_new (GtkSnapLayoutChild, 1);
  child_info->widget = widget;
  child_info->x = 0;
  child_info->y = 0;

  gtk_widget_set_parent (widget, GTK_WIDGET (snaplayout));

  snaplayout->children = g_list_append (snaplayout->children, child_info);
}

static void
gtk_snaplayout_move_internal (GtkSnapLayout       *snaplayout,
                         GtkWidget      *widget,
                         gboolean        change_x,
                         gint            x,
                         gboolean        change_y,
                         gint            y)
{
  GtkSnapLayoutChild *child;

  g_return_if_fail (GTK_IS_SNAPLAYOUT (snaplayout));
  g_return_if_fail (GTK_IS_WIDGET (widget));
  g_return_if_fail (widget->parent == GTK_WIDGET (snaplayout));

  child = get_child (snaplayout, widget);

  g_assert (child);

  /*gtk_widget_freeze_child_notify (widget);*/

  if (change_x)
    {
      child->x = x;
      /*gtk_widget_child_notify (widget, "x");*/
    }

  if (change_y)
    {
      child->y = y;
      /*gtk_widget_child_notify (widget, "y");*/
    }

  /*gtk_widget_thaw_child_notify (widget);*/

  if (gtk_widget_get_visible (widget) &&
      gtk_widget_get_visible (GTK_WIDGET (snaplayout)))
    gtk_widget_queue_resize (GTK_WIDGET (snaplayout));
}

void
gtk_snaplayout_move (GtkSnapLayout       *snaplayout,
                GtkWidget      *widget,
                gint            x,
                gint            y)
{
  gtk_snaplayout_move_internal (snaplayout, widget, TRUE, x, TRUE, y);
}
/*
static void
gtk_snaplayout_set_child_property (GtkContainer    *container,
                              GtkWidget       *child,
                              guint            property_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
  switch (property_id)
    {
    case CHILD_PROP_X:
      gtk_snaplayout_move_internal (GTK_SNAPLAYOUT (container),
                               child,
                               TRUE, g_value_get_int (value),
                               FALSE, 0);
      break;
    case CHILD_PROP_Y:
      gtk_snaplayout_move_internal (GTK_SNAPLAYOUT (container),
                               child,
                               FALSE, 0,
                               TRUE, g_value_get_int (value));
      break;
    default:
      GTK_CONTAINER_WARN_INVALID_CHILD_PROPERTY_ID (container, property_id, pspec);
      break;
    }
}

static void
gtk_snaplayout_get_child_property (GtkContainer *container,
                              GtkWidget    *child,
                              guint         property_id,
                              GValue       *value,
                              GParamSpec   *pspec)
{
  GtkSnapLayoutChild *fixed_child;

  fixed_child = get_child (GTK_SNAPLAYOUT (container), child);

  switch (property_id)
    {
    case CHILD_PROP_X:
      g_value_set_int (value, fixed_child->x);
      break;
    case CHILD_PROP_Y:
      g_value_set_int (value, fixed_child->y);
      break;
    default:
      GTK_CONTAINER_WARN_INVALID_CHILD_PROPERTY_ID (container, property_id, pspec);
      break;
    }
}
*/

static void
gtk_snaplayout_realize (GtkWidget *widget)
{
	GdkWindowAttr attributes;
	gint attributes_mask;

	if (!gtk_widget_get_has_window (widget))
		GTK_WIDGET_CLASS (gtk_snaplayout_parent_class)->realize (widget);
	else
	{
		gtk_widget_set_realized (widget, TRUE);

		attributes.window_type = GDK_WINDOW_CHILD;
		attributes.x = widget->allocation.x;
		attributes.y = widget->allocation.y;
		attributes.width = widget->allocation.width;
		attributes.height = widget->allocation.height;
		attributes.wclass = GDK_INPUT_OUTPUT;
		attributes.visual = gtk_widget_get_visual (widget);
		attributes.colormap = gtk_widget_get_colormap (widget);
		attributes.event_mask = gtk_widget_get_events (widget);
		attributes.event_mask |= GDK_EXPOSURE_MASK | GDK_BUTTON_PRESS_MASK;

		attributes_mask = GDK_WA_X | GDK_WA_Y | GDK_WA_VISUAL | GDK_WA_COLORMAP;

		widget->window = gdk_window_new (gtk_widget_get_parent_window (widget), &attributes, attributes_mask);
		gdk_window_set_user_data (widget->window, widget);

		widget->style = gtk_style_attach (widget->style, widget->window);
		gtk_style_set_background (widget->style, widget->window, GTK_STATE_NORMAL);
	}
}

static void
gtk_snaplayout_size_request (GtkWidget      *widget,
			GtkRequisition *requisition)
{
	GtkSnapLayout *snaplayout;
	GtkSnapLayoutChild *child;
	GList *children;
	GtkRequisition child_requisition;

	snaplayout = GTK_SNAPLAYOUT(widget);
	requisition->width = 0;
	requisition->height = 0;

	children = snaplayout->children;
	while (children)
	{
		child = children->data;
		children = children->next;

		if (gtk_widget_get_visible(child->widget))
		{
			gtk_widget_size_request(child->widget, &child_requisition);

			requisition->height = MAX(requisition->height,	child->y + child_requisition.height);
			requisition->width = MAX(requisition->width, child->x + child_requisition.width);
		}
	}

	/*requisition->height += GTK_CONTAINER (snaplayout)->border_width * 2;
	requisition->width += GTK_CONTAINER (snaplayout)->border_width * 2;
	*/
}

static void gtk_snaplayout_size_allocate (GtkWidget *widget, GtkAllocation *allocation)
{
	GtkSnapLayout *snaplayout;
	GtkSnapLayoutChild *child;
	GtkAllocation child_allocation;
	GtkRequisition child_requisition;
	GList *children;
	/*guint16 border_width;*/

	snaplayout = GTK_SNAPLAYOUT (widget);

	widget->allocation = *allocation;

	if (gtk_widget_get_has_window(widget))
	{
		if (gtk_widget_get_realized (widget))
			gdk_window_move_resize (widget->window,
		allocation->x,
		allocation->y,
		allocation->width,
		allocation->height);
	}

	/*border_width = GTK_CONTAINER(snaplayout)->border_width;*/

	children = snaplayout->children;
	while (children)
	{
		child = children->data;
		children = children->next;

		if (gtk_widget_get_visible(child->widget))
		{
			gtk_widget_get_child_requisition(child->widget, &child_requisition);
			child_allocation.x = (allocation->width / 2) - (child_requisition.width / 2);
			child_allocation.y = (allocation->height / 2) - (child_requisition.height / 2);

			if (!gtk_widget_get_has_window(widget))
			{
				child_allocation.x += widget->allocation.x;
				child_allocation.y += widget->allocation.y;
			}

			child_allocation.width = child_requisition.width;
			child_allocation.height = child_requisition.height;
			gtk_widget_size_allocate(child->widget, &child_allocation);
		}
	}
}

static void
gtk_snaplayout_add (GtkContainer *container,
	       GtkWidget    *widget)
{
  gtk_snaplayout_put (GTK_SNAPLAYOUT (container), widget);
}

static void
gtk_snaplayout_remove (GtkContainer *container,
		  GtkWidget    *widget)
{
  GtkSnapLayout *snaplayout;
  GtkSnapLayoutChild *child;
  GtkWidget *widget_container;
  GList *children;

  snaplayout = GTK_SNAPLAYOUT (container);
  widget_container = GTK_WIDGET (container);

  children = snaplayout->children;
  while (children)
    {
      child = children->data;

      if (child->widget == widget)
	{
	  gboolean was_visible = gtk_widget_get_visible (widget);

	  gtk_widget_unparent (widget);

	  snaplayout->children = g_list_remove_link (snaplayout->children, children);
	  g_list_free (children);
	  g_free (child);

	  if (was_visible && gtk_widget_get_visible (widget_container))
	    gtk_widget_queue_resize (widget_container);

	  break;
	}

      children = children->next;
    }
}

static void
gtk_snaplayout_forall (GtkContainer *container,
		  gboolean	include_internals,
		  GtkCallback   callback,
		  gpointer      callback_data)
{
  GtkSnapLayout *snaplayout = GTK_SNAPLAYOUT (container);
  GtkSnapLayoutChild *child;
  GList *children;

  children = snaplayout->children;
  while (children)
    {
      child = children->data;
      children = children->next;

      (* callback) (child->widget, callback_data);
    }
}



