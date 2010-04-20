Java Custom Uploader
=

__About__: A simple, Java-based file uploader with a clean UI.  It can run as an
applet on your website (Java 1.4 or higher) or as part of a desktop application.
This software is open-source and free to use or include in your own project.

The upload logic is pluggable, so you can use one of the included upload
mechanisms (e.g., HTTP) _or roll your own_ to upload via whatever protocol you
need.  The uploader automatically parallelizes uploads and provides the user
with feedback on each upload's progress (including errors).

The [__JCustomUploader homepage__](http://www.dound.com/projects/jcustomuploader/) 
has links to demos, screenshots, and more information about JCustomUploader's 
features.  This readme is intended to help you get started with JCustomUploader.

Problem?  Please report it on the [JCustomUploader issues
page](http://github.com/dound/JCustomUploader/issues).


Usage
-
First download JCustomUploader - you can either get the source code, or the
latest pre-compiled jar from the [downloads](http://github.com/dound/JCustomUploader/downloads)
page.  To use the uploader, you will need to instantiate an UploaderPanel.  With
its constructor you specify:

  - How to upload files and how many uploads to perform in parallel.
  - What kinds of files may be selected.
  - Whether to show a preview of the selected file (only for images).
  - How text in the UI refers to what you are uploading, e.g., "photo" or "item".


**Using it as an Applet:**

  1. In AppletDemo.java, modify the arguments to UploaderPanel() to suit
     your needs.
  1. Build and sign the jar (the included ant build script, [build.xml](http://github.com/dound/JCustomUploader/blob/master/build.xml), does this).
  1. Add it to your webpage just like (this demo page)[http://github.com/downloads/dound/JCustomUploader/demo-applet-v1.0.html].


**Using it inside your project:**

  1. Either reference the jar containing the JCustomUploader or copy the code
     into your source folder.
  1. Create an UploaderPanel (as above) and display it in a dialog or some
     other Swing container.  [BasicSmugMugUploader](http://github.com/dound/JCustomUploader/blob/master/src/uploader/demo/BasicSmugMugUploader.java)
     demonstrates this approach.


Example Usage
-
This example uploader uses 3 threads and only lets the user upload images:

    // obviously you'll want to do this in a method on your applet/application

    // Build the uploaders to upload to http://127.0.0.1/upload/ using HTTP POST.
    final int NUM_THREADS = 3;
    final UploadMechanism[] uploadMechs = new UploadMechanism[NUM_THREADS];
    int maxSideLen = 1024;
    for(int i=0; i<NUM_THREADS; i++) {
        uploadMechs[i] = new HTTPUploadMechanism("127.0.0.1", "/upload/");

        // override the default which just sends the file as-is
        uploadMechs[i].setUploadFileGetter(new ScaledImageGetter(maxSideLen));
    }

    // just use the width which the applet was given
    int width = getWidth();

    // what kind of things we're uploading (displayed to the user on various
    // parts of the UI)
    String itemType = "photo";

    // only let the user select images
    FileFilter filter = new ImageFileFilter();

    // show a preview of files in the file chooser dialog box (only for images)
    boolean showPreview = true;

    // create the uploader UI (it starts the background uploader threads)
    UploaderPanel newContentPane = new UploaderPanel(width, uploadMechs, itemType, filter, showPreview);


Limitations
-
  * The provided text is in English with no easy hook to provide a custom
    translation.
  * Primarily intended to interact with a user, not a program (e.g., there is
    not great support for pragmatically stop an upload, etc.).  The applet does
    not expose any hooks which can be called by JavaScript.


_Author_: [David Underhill](http://www.dound.com)  
_Release Date_: 2010-Apr-20 (v1.0)  
_License_: Apache License Version 2.0  
