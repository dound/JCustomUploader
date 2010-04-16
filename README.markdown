Java Custom Uploader
=

__About__: A simple, Java-based file uploader with a clean UI.  It can run as an
applet on your website (Java 1.5 or higher) or as part of a desktop application.

The upload logic is pluggable, so you can either write your own (upload over any
protocol you want!) or use one of the included upload mechanisms (e.g., HTTP).
The uploader handles automatically parallelizing uploads and providing the user
with feedback in the case of an error.

Problem?  Please report it on the [JCustomUploader issues
page](http://github.com/dound/JCustomUploader/issues).


Advantages
-
  * __Clean, Functional UI__: The user can select multiple files at a time or
    even folders.  Uploads can be canceled at any time, or retried if they
    fail.  Information about the overall upload progress and expected completion
    time is updated as uploads progress and as the user adds files.
  * __Pluggable Upload Mechanism__: JCustomUploader can upload files using any
    means you like.  You just extend [AbstractUploadMechanism](http://github.com/dound/JCustomUploader/blob/master/src/uploader/mechanisms/AbstractUploadMechanism.java)
    which specifies how to start an upload and send a chunk of data.
    JCustomUploader handles getting the data to you, canceling uploads, and
    performing multiple uploads in parallel.  It comes with several such mechanisms:
    - HTTP (raw binary data)
    - HTTP (multipart/form-data encoded)
    - Test (uploads nowhere - useful for testing)
  * __Parallel Uploads__: Multiple uploads can be sent in parallel - you can
    choose how many uploads will be done at once.
  * __Java 1.5__: Only needs Java 1.5 (aka Java 5) or higher to run.


Limitations
-
  * The provided text is in English with no easy hook to provide a custom
    translation.
  * Primarily intended to interact with a user, not a program (there aren't many
    hooks for you to pragmatically stop an upload, etc.).


Usage
-
First download and unpack JCustomUploader.  See UploaderApplet.java's
createGUI() method for an example of how to initialize the uploader (whether or
not you use it in an applet).  In particular, the arguments to UploaderPanel()
are used to customize how JCustomUploader works for you:

  - How to upload files and how many uploads to perform in parallel.
  - How text in the UI refers to what your uploading, e.g., "photo" or "item".
  - What kinds of files may be selected:
  - Whether to show a preview of the selected file (only for images).


**Using it as an Applet:**

  1. In UploaderApplet.java, modify the arguments to UploaderPanel() to suit
     your needs.
  1. Build and sign the jar (details coming soon).
  1. Add it to your webpage (details coming soon).


**Using it inside your project:**

  1. Either reference the jar containing the JCustomUploader or copy the code
     into your source folder.
  1. Create an UploaderPanel() (as above) and display it in a dialog or some
     other Swing container.


Example Usage
-

This example uploader uses three threads and only lets the user upload images.
A test upload mechanism which just pretends to upload files is used here for
demonstration purposes.

    // obviously you'll want to do this in a method on your applet/application

    // Build the uploaders (the test uploader is configured here to fail to
    // start uploading 20% of files and fail in the midst of "sending" a piece
    // of a file 1% of the time - this is to demo how failures are handled.)
    final int NUM_THREADS = 3;
    final UploadMechanism[] uploadMechs = new UploadMechanism[NUM_THREADS];
    for(int i=0; i<NUM_THREADS; i++)
        uploadMechs[i] = new TestUploadMechanism(250, 0.2, 0.01, System.currentTimeMillis()+i*100);

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


_Author_: [David Underhill](http://www.dound.com)  
_Release Date_: 2010-Apr-15 (v1.00)  
_License_: Apache License Version 2.0  
