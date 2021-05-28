package net.intensecorp.meeteazy.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class ApiUtility {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static final String AUTHORIZATION_KEY = "key=AAAAaKi555Q:APA91bHiPMazAftZK4dGMOYy-sPFgvAI7BN-nl-5YOar-kX_HwP6OU0VdmdO_KjzjDIbsDahLAvyDde61M4SvhBZnBbcy9CNb-LXUMLrKg0ahbDMfmX2o-tmplkwFviE88Im0jPoS10g";
    private static final String CONTENT_TYPE_JSON = "application/json";

    public static final String JSON_OBJECT_DATA = "data";
    public static final String JSON_OBJECT_REGISTRATION_IDS = "registration_ids";

    public static final String KEY_MESSAGE_TYPE = "messageType";
    public static final String KEY_REQUEST_TYPE = "requestType";
    public static final String KEY_RESPONSE_TYPE = "responseType";
    public static final String KEY_CALL_TYPE = "callType";
    public static final String KEY_CALLER_FIRST_NAME = "callerFirstName";
    public static final String KEY_CALLER_LAST_NAME = "callerLastName";
    public static final String KEY_CALLER_EMAIL = "callerEmail";
    public static final String KEY_CALLER_PROFILE_PICTURE_URL = "callerProfilePictureUrl";
    public static final String KEY_ROOM_ID = "roomId";
    public static final String KEY_CALLER_FCM_TOKEN = "callerToken";

    public static final String MESSAGE_TYPE_CALL_REQUEST = "callRequest";
    public static final String MESSAGE_TYPE_CALL_RESPONSE = "callResponse";
    public static final String REQUEST_TYPE_ENDED = "ended";
    public static final String REQUEST_TYPE_INITIATED = "initiated";
    public static final String RESPONSE_TYPE_ANSWERED = "answered";
    public static final String RESPONSE_TYPE_REJECTED = "rejected";
    public static final String CALL_TYPE_PERSONAL = "personalCall";
    public static final String CALL_TYPE_GROUP = "groupCall";

    private static final String SERVER_URL_JITSI_MEET = "https://meet.jit.si";

    public static HashMap<String, String> getMessageHeaders() {
        HashMap<String, String> messageHeaders = new HashMap<>();
        messageHeaders.put(HEADER_AUTHORIZATION, AUTHORIZATION_KEY);
        messageHeaders.put(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);

        return messageHeaders;
    }

    public static URL getJitsiMeetServerUrl() {
        URL serverUrl = null;
        try {
            serverUrl = new URL(SERVER_URL_JITSI_MEET);
            return serverUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return serverUrl;
    }
}
