#ifndef GTKSNAPLAYOUT_H_INCLUDED
#define GTKSNAPLAYOUT_H_INCLUDED

/*
adamb (2012-06-09)
    The GtkSnapLayout class was adapted from the GtkFixed code (GTK 2.24).
    If future GTK versions cause breakage, I suggest you compare the changes
    which were made to GtkFixed and apply them to this class as needed.

*/

#include <gtk/gtkcontainer.h>


G_BEGIN_DECLS

#define GTK_TYPE_SNAPLAYOUT                  (gtk_snaplayout_get_type ())
#define GTK_SNAPLAYOUT(obj)                  (G_TYPE_CHECK_INSTANCE_CAST ((obj), GTK_TYPE_SNAPLAYOUT, GtkSnapLayout))
#define GTK_SNAPLAYOUT_CLASS(klass)          (G_TYPE_CHECK_CLASS_CAST ((klass), GTK_TYPE_SNAPLAYOUT, GtkSnapLayoutClass))
#define GTK_IS_SNAPLAYOUT(obj)               (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GTK_TYPE_SNAPLAYOUT))
#define GTK_IS_SNAPLAYOUT_CLASS(klass)       (G_TYPE_CHECK_CLASS_TYPE ((klass), GTK_TYPE_SNAPLAYOUT))
#define GTK_SNAPLAYOUT_GET_CLASS(obj)        (G_TYPE_INSTANCE_GET_CLASS ((obj), GTK_TYPE_SNAPLAYOUT, GtkSnapLayoutClass))


typedef struct _GtkSnapLayout        GtkSnapLayout;
typedef struct _GtkSnapLayoutClass   GtkSnapLayoutClass;
typedef struct _GtkSnapLayoutChild   GtkSnapLayoutChild;

struct _GtkSnapLayout
{
  GtkContainer container;

  GList *GSEAL (children);
};

struct _GtkSnapLayoutClass
{
  GtkContainerClass parent_class;
};

struct _GtkSnapLayoutChild
{
  GtkWidget *widget;
  gint x;
  gint y;
};


GType      gtk_snaplayout_get_type          (void) G_GNUC_CONST;
GtkWidget* gtk_snaplayout_new               (void);

void       gtk_snaplayout_put(GtkSnapLayout *snaplayout, GtkWidget *widget);

/*void       gtk_snaplayout_move              (GtkSnapLayout       *snaplayout,
                                        GtkWidget      *widget,
                                        gint            x,
                                        gint            y);*/


G_END_DECLS

#endif // GTKSNAPLAYOUT_H_INCLUDED
