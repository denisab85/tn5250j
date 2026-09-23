package org.tn5250j.session.wire;

public final class WsMessageType {

    public static final String HOST_HELLO = "HostHello";
    public static final String HOST_HELLO_ACK = "HostHelloAck";
    public static final String SESSION_OPEN = "SessionOpen";
    public static final String SESSION_CLOSE = "SessionClose";
    public static final String SESSION_ATTACH = "SessionAttach";
    public static final String CONNECT = "Connect";
    public static final String DISCONNECT = "Disconnect";
    public static final String SEND_KEYS = "SendKeys";
    public static final String PASTE_TEXT = "PasteText";
    public static final String MOVE_CURSOR = "MoveCursor";
    public static final String SET_CURSOR = "SetCursor";
    public static final String SEND_AID = "SendAid";
    public static final String GET_SNAPSHOT = "GetSnapshot";
    public static final String SCREEN_REGION_UPDATED = "ScreenRegionUpdated";
    public static final String SCREEN_SIZE_CHANGED = "ScreenSizeChanged";
    public static final String OIA_CHANGED = "OiaChanged";
    public static final String SESSION_STATE_CHANGED = "SessionStateChanged";
    public static final String BELL = "Bell";
    public static final String UI_PROMPT_REQUEST = "UiPromptRequest";
    public static final String UI_PROMPT_RESPONSE = "UiPromptResponse";
    public static final String RPC = "Rpc";
    public static final String REPLY = "Reply";
    public static final String ERROR = "Error";

    private WsMessageType() {
    }
}
