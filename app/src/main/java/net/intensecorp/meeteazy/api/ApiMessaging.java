package net.intensecorp.meeteazy.api;

import java.util.HashMap;

public class ApiMessaging {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    // Paste your Authorization Key here
    private static final String AUTHORIZATION_KEY = "your_authorization_key";
    private static final String CONTENT_TYPE_JSON = "application/json";

    public static final String JSON_OBJECT_DATA = "data";
    public static final String JSON_OBJECT_REGISTRATION_IDS = "registration_ids";

    public static final String KEY_MESSAGE_TYPE = "messageType";
    public static final String KEY_CALL_TYPE = "callType";
    public static final String KEY_CALLER_FIRST_NAME = "callerFirstName";
    public static final String KEY_CALLER_LAST_NAME = "callerLastName";
    public static final String KEY_CALLER_EMAIL = "callerEmail";
    public static final String KEY_CALLER_PROFILE_PICTURE_URL = "callerProfilePictureUrl";
    public static final String KEY_MEMBER_COUNT = "memberCount";
    public static final String KEY_MEETING_ROOM_NAME = "meetingRoomName";
    public static final String KEY_CALLER_FCM_TOKEN = "callerToken";

    public static final String MESSAGE_TYPE_CALL_REQUEST = "callRequest";
    public static final String MESSAGE_TYPE_CALL_RESPONSE = "callResponse";
    public static final String REQUEST_ENDED = "ended";
    public static final String RESPONSE_ANSWERED = "answered";
    public static final String RESPONSE_REJECTED = "rejected";
    public static final String TYPE_VOICE_CALL = "voiceCall";
    public static final String TYPE_VIDEO_CALL = "videoCall";
    public static final String TYPE_GROUP_VOICE_CALL = "groupVoiceCall";
    public static final String TYPE_GROUP_VIDEO_CALL = "groupVideoCall";

    public static HashMap<String, String> getMessageHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(HEADER_AUTHORIZATION, AUTHORIZATION_KEY);
        headers.put(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);

        return headers;
    }
}
