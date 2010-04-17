package uploader.mechanisms;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import uploader.mechanisms.event.HTTPUploadListener;
import uploader.mechanisms.event.SmugMugUploadListener;
import uploader.util.Util;

/**
 * Uploads files to SmugMug.  Uploads will be posted to HOST:PORT with the
 * Request-URI set to PATH (HTTP/1.0).  The request will send the following
 * headers: Host, X-Smug-Response, X-Smug-FileName, Content-MD5, X-Smug-AlbumID,
 * X-Smug-Version (1.2.2), and a header which authenticates the uploader (either
 * X-Smug-SessionID or an OAuth-compliant Authorization header).  If OAuth is
 * used, oauth_signature will be encoded using the HMAC-SHA1 scheme.
 *
 * Use SmugMugUploadListener to be notified of successful uploads (and get the
 * uploaded image's SmugMug ID, Key, and URL).
 *
 * @author David Underhill
 */
public class SmugMugUploadMechanism extends HTTPUploadMechanism {
    private static final Random rand = new Random();

    // information about the SmugMug API relevant to uploading
    private static final String HOST = "upload.smugmug.com";
    private static final int PORT = 80;
    private static final String METHOD = "POST";
    private static final String PATH = "/photos/xmlrawadd.mg";
    private static final String HEADERS = "Host: " + HOST + "\r\nX-Smug-Response: REST\r\nX-Smug-Version: 1.2.2\r\nUser-Agent: JCustomUploader\r\n";

    /** OAuth parameters (null if authentication is being done via sessions */
    private final String apiKey, consumerSecret, accessToken, accessTokenSecret;

    /** an object (if any) to notify whenever a response completes */
    private volatile SmugMugUploadListener eventListener = null;

    /**
     * Constructs a SmugMug upload mechanism which will authenticate the
     * uploader using X-Smug-SessionID.
     *
     * @param albumID       the album to put the uploaded file in
     * @param sessionID     the session ID to authenticate with
     * @param extraHeaders  optional additional headers (e.g., X-Smug-Keywords)
     */
    public SmugMugUploadMechanism(String albumID, String sessionID, String extraHeaders) {
        this(albumID, "X-Smug-SessionID: " + sessionID + "\r\n"+extraHeaders, null, null, null, null);
    }
    public SmugMugUploadMechanism(String albumID, String sessionID) {
        this(albumID, sessionID, "");
    }

    /**
     * Constructs a SmugMug upload mechanism which will authenticate the
     * uploader using X-Smug-SessionID.
     *
     * @param albumID            the album to put the uploaded file in
     * @param apiKey             your SmugMug API key (aka Consumer Key)
     * @param consumerSecret     the secret associated with your SmugMug API Key
     * @param accessToken        the access token to use to upload photos
     * @param accessTokenSecret  the secret associated with the access token
     * @param extraHeaders       optional additional headers (e.g., X-Smug-Keywords)
     */
    public SmugMugUploadMechanism(String albumID, String apiKey, String consumerSecret, String accessToken, String accessTokenSecret, String extraHeaders) {
        super(HOST, PORT, METHOD, PATH, HEADERS+extraHeaders+"X-Smug-AlbumID: "+albumID+"\r\n", false, null);
        super.setEventListener(new MyHTTPUploadListener());
        this.apiKey = apiKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;
    }
    public SmugMugUploadMechanism(String albumID, String apiKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        this(albumID, apiKey, consumerSecret, accessToken, accessTokenSecret, "");
    }

    // regular expressions for parsing out some info from SmugMug's responses
    private static final Pattern RE_RESP_STAT  = Pattern.compile("<rsp stat=\"([^\"]+)\">");
    private static final Pattern RE_RESP_ERR   = Pattern.compile("msg=\"(.+)\"");
    private static final Pattern RE_RESP_DATA  = Pattern.compile("<Image id=\"([^\"]+)\" Key=\"([^\"]+)\" URL=\"([^\"]+)\"/>");

    /**
     * Checks the response to an upload and fires the SmugMug event listener (if
     * any) on success.  Returns an error message if the response indicates that
     * the upload failed.
     */
    private class MyHTTPUploadListener implements HTTPUploadListener {
        public String responseReceived(HTTPUploadMechanism u, int code, String resp) {
            String stat = "fail";
            Matcher match_stat = RE_RESP_STAT.matcher(resp);
            if(match_stat.find())
                stat = match_stat.group(1);

            if(stat.equalsIgnoreCase("ok")) {
                // upload succeeded: get the info about the image on their servers that they send back
                SmugMugUploadListener listener = eventListener;
                if(listener != null) {
                    Matcher match_data = RE_RESP_DATA.matcher(resp);
                    if(match_data.find())
                        listener.responseReceived(SmugMugUploadMechanism.this, match_data.group(1), match_data.group(2), match_data.group(3));
                    else
                        System.err.println("Warning: could not find image ID, key, and URL in SmugMug response: " + resp);
                }
                return null;
            }
            else {
                // SmugMug indicated failure - get their error message if they sent one
                Matcher match_err = RE_RESP_ERR.matcher(resp);
                if(match_err.find())
                    return match_err.group(1);
                else
                    return "SmugMug indicated failure";
            }
        }
    }

    /**
     * Returns X-Smug-FileName and Content-MD5 headers for f.
     */
    protected String getAdditionalHeaders(File f) {
        String oauthHeaderIfNeeded;
        try {
            oauthHeaderIfNeeded = makeOAuthHeaders();
        }
        catch(Exception e) {
            haltWithError(e.getMessage());
            return null;
        }

        String xsmugfn = "X-Smug-FileName: " + f.getName() + "\r\n";
        String md5;
        try {
            md5 = Util.md5(f);
        }
        catch(Exception e) {
            // send it without the md5 sum if we can't compute it
            return super.getAdditionalHeaders(f) + oauthHeaderIfNeeded + xsmugfn;
        }
        String contentMD5 = "Content-MD5: " + md5 + "\r\n";
        return super.getAdditionalHeaders(f) + oauthHeaderIfNeeded + xsmugfn + contentMD5;
    }

    private String makeOAuthHeaders() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        // build the arguments to the HMAC-SHA1 function
        String key = consumerSecret + "&" + accessTokenSecret;
        String baseURL = Util.urlencode("http://" + HOST + PATH);
        int nonce = rand.nextInt();
        long timestamp = System.currentTimeMillis() / 1000;
        String quoted_params = Util.urlencode("oauth_consumer_key=" + apiKey + "&" +
                                              "oauth_nonce=" + nonce + "&" +
                                              "oauth_signature_method=HMAC-SHA1&" +
                                              "oauth_timestamp=" + timestamp + "&" +
                                              "oauth_token=" + accessToken + "&" +
                                              "oauth_version=1.0");
        String text = METHOD + "&" + baseURL + "&" + quoted_params;

        // compute the HMAC-SHA1 signature
        SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(sks);
        String base64sig = new String(Util.base64bytes( mac.doFinal(text.getBytes()) ));
        String sig = Util.urlencode(base64sig);

        // build and return the authorization header
        return "Authorization: OAuth realm=\"http://" + HOST +"/\", " +
               "oauth_consumer_key=\""     + apiKey      + "\", " +
               "oauth_token=\""            + accessToken + "\", " +
               "oauth_signature_method=\"" + "HMAC-SHA1" + "\", " +
               "oauth_signature=\""        + sig         +  "\", " +
               "oauth_timestamp=\""        + timestamp   + "\", " +
               "oauth_nonce=\""            + nonce       + "\", " +
               "oauth_version=\"1.0\"\r\n";
    }

    /** gets the object listening for SmugMug upload events (may be null) */
    public SmugMugUploadListener getSmugMugEventListener() {
        return eventListener;
    }

    /**
     * Sets the object to be notified of SmugMug upload events (responses).  The
     * callbacks will be issued from an upload thread NOT the Swing EDT.  Use
     * SwingUtilities.invokeLater() if you need to run on the EDT.
     *
     * This method is thread-safe (i.e., you can set the event listener from
     * any thread).
     */
    public void setSmugMugEventListener(SmugMugUploadListener l) {
        eventListener = l;
    }
}
