Standard GUI (stdgui)
======

<pre style="font-family: sans; font-size: large; font-style: italic">
Create a graphical user interface,
    With your favorite language,
		That looks native,
			On any platform,
				And do it quickly!
</pre>

Background
----------

Why is it that writing a GUI program is so much more complex than writing a console program?  Console user interfaces are so darn simple:

* You output text through stdout.
* You read text from stdin.
* Arguments are passed to you in a string array.
* All major platforms *and* programming languages support this, out-of-the-box!  No additional libraries required.

So, if you wish to write a program, and you want it to be as portable as possible, writing a console program is the simplest solution.

Things are not so simple with GUI programs.  Here's a list of things that must be considered:

* What language am I working in?  Which GUI toolkits have bindings for my language?
* What platform(s) am I targeting?  Which toolkits are supported on these platforms?
* How can I make my program look and feel like a native program?
* How do I do *X* with my chosen toolkit?
* Why are the language bindings incomplete and poorly documented?
* Does my GUI meet accessibility standards for all platforms?
* Why am I writing so much code just for a GUI!

You get the point.  Creating a cross platform GUI program is not simple.  Perhaps we can change this.  I don't think there's any technical reason we cannot have some sort of abstract, standardized GUI programming model as ubiquitous as stdin/stdout.  We just have to make a few concessions:

* We must give up power and flexibility in order to gain simplicity and portability.
* Avoid the huge API, "toolkit" paradigm and instead use a specialized middle-man language to define the GUI.

So, if you need to write an advanced GUI program, look elsewhere, stdgui is not for you.  However if you want to write a portable GUI program with minimal fuss, stdgui is the solution.

[Read more](http://binary-mind.blogspot.com/2011/03/standard-gui-stdgui.html)

TODO: the above really belongs in a separate document.  The readme is for getting people started quickly.
