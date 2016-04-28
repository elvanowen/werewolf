import org.json.simple.JSONObject;

import java.net.DatagramPacket;

/**
 * Created by elvan_owen on 4/27/16.
 */
public interface OnMessageResponseInterface {
    public void onMessageReceived(JSONObject message);
    public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort);
}
