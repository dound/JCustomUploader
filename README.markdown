Java Custom Uploader
=

__About__: A simple, Java-based file uploader with a clean UI.  It can run as an
applet on your website (Java 1.5 or higher) or as part of a desktop application.

The upload logic is pluggable, so you can use one of the included upload
mechanisms (e.g., HTTP) _or roll your own_ to upload via whatever protocol you
need.  The uploader handles automatically parallelizing uploads and providing
the user with feedback in the case of an error.

__Demo__: Several examples have been put together:

  1. An applet running the uploader will be posted soon.
  1. A [basic SmugMug uploader](http://github.com/dound/JCustomUploader/blob/master/src/uploader/demo/BasicSmugMugUploader.java)
     has been put together to demonstrate the uploader being used as part of a larger application.
  1. Check out the example at the bottom of this document.


Problem?  Please report it on the [JCustomUploader issues
page](http://github.com/dound/JCustomUploader/issues).


Features
-
  * __Clean, Functional UI__: The user can select multiple files at a time or
    even folders.  Uploads can be canceled at any time, or retried if they
    fail.  Information about the overall upload progress and expected completion
    time is updated as uploads progress and as the user adds files.
  * __Pluggable Upload Mechanism__: JCustomUploader can upload files using any
    means you like.  You just extend [AbstractUploadMechanism](http://github.com/dound/JCustomUploader/blob/master/src/uploader/mechanisms/AbstractUploadMechanism.java)
    which specifies how to start an upload and send a chunk of data.
    JCustomUploader handles getting the data to you, canceling uploads, and
    performing multiple uploads in parallel.  It comes with several built-in
    mechanisms:
    - HTTP (raw binary data)
    - HTTP (multipart/form-data encoded)
    - SmugMug API (with OAuth or SmugMug sessions)
    - Test (uploads nowhere - useful for testing)
  * __Custom Hooks__: You can register for callbacks which allow you to
    pre-process files before uploading them or do some extra processing on the
    server's response to each upload.
    - A pre-processor for images which resizes them before uploading them is
      included.  The example below demonstrates how to use this component.
  * __Parallel Uploads__: Multiple uploads can be sent in parallel - you just
    specify the maximum number to try at once and JCustomUploader does the rest.
  * __Java 1.5__: Only needs Java 1.5 (aka Java 5) or higher to run.


Limitations
-
  * The provided text is in English with no easy hook to provide a custom
    translation.
  * Primarily intended to interact with a user, not a program (e.g., there is
    not great support for pragmatically stop an upload, etc.).  The applet does
    not expose any hooks which can be called by JavaScript.


Usage
-
First download and unpack JCustomUploader.  To use the uploader, you will need
to instantiate an UploaderPanel.  With its constructor you specify:

  - How to upload files and how many uploads to perform in parallel.
  - What kinds of files may be selected.
  - Whether to show a preview of the selected file (only for images).
  - How text in the UI refers to what you are uploading, e.g., "photo" or "item".


**Using it as an Applet:**

  1. In UploaderApplet.java, modify the arguments to UploaderPanel() to suit
     your needs.
  1. Build and sign the jar (details coming soon).
  1. Add it to your webpage (details coming soon).


**Using it inside your project:**

  1. Either reference the jar containing the JCustomUploader or copy the code
     into your source folder.
  1. Create an UploaderPanel (as above) and display it in a dialog or some
     other Swing container.  [BasicSmugMugUploader](http://github.com/dound/JCustomUploader/blob/master/src/uploader/demo/BasicSmugMugUploader.java)
     demonstrates this approach (for details, see its documentation).


Example Usage
-
This example uploader uses three threads and only lets the user upload images:

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


_Author_: [David Underhill](http://www.dound.com)  
_Release Date_: 2010-Apr-17 (pre-release)  
_License_: Apache License Version 2.0  
