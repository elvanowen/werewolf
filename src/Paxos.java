import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by elvan_owen on 4/28/16.
 */
enum PAXOS_STATE {
    NONE,
    PREPARE,
    ACCEPT,
    DONE
}

enum PAXOS_ROLE {
    LEADER,
    ACCEPTOR
}

public class Paxos {
    PAXOS_STATE state = PAXOS_STATE.NONE;
    PAXOS_ROLE role;
    int proposalSequenceNumber = 0;
    int playerID;
    int kpuID;
    ArrayList<JSONObject> clientList;
    ArrayList<JSONObject> preparePromiseList = new ArrayList<>();
    ArrayList<JSONObject> acceptPromiseList = new ArrayList<>();
    Integer[] highestProposalId = new Integer[2];
    OnLeaderChosenInterface onLeaderChosenCallback;
    TCPClient serverSocket;
    UDPServer udpServer;
    static boolean isCallbackCalled;

    public Paxos(PAXOS_ROLE role){
        this.role = role;

        setAcceptedKpuID(-1);
        setHighestProposalID(-1, -1);

        System.out.println("Paxos contructor : " + role);
    }

    void setPlayerID(int playerID){
        this.playerID = playerID;
    }

    void setClientList(ArrayList<JSONObject> clientList){
        this.clientList = clientList;
    }

    int getAcceptedKpuID(){
        return kpuID;
    }

    void setAcceptedKpuID(int kpuID){
        this.kpuID = kpuID;
    }

    void setHighestProposalID(String sequenceNumber, String playerID){
        this.highestProposalId[0] = Integer.parseInt(sequenceNumber);
        this.highestProposalId[1] = Integer.parseInt(playerID);
    }

    void setHighestProposalID(int sequenceNumber, int playerID){
        this.highestProposalId[0] = sequenceNumber;
        this.highestProposalId[1] = playerID;
    }

    void setServerSocket(TCPClient client){
        this.serverSocket = client;
    }

    void setUDPServer(UDPServer udpServer){
        this.udpServer = udpServer;
    }

    Integer[] getHighestProposalId(){
        return this.highestProposalId;
    }

    void sendPrepareProposal(){
        state = PAXOS_STATE.PREPARE;

//        Reset all value
        preparePromiseList = new ArrayList<>();
//        acceptPromiseList = new ArrayList<>();
        highestProposalId = new Integer[2];

        if (role == PAXOS_ROLE.LEADER){

//            Added delay to make sure every node is up
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "prepare_proposal");

            JSONArray proposalId =  new JSONArray();
            proposalId.add(++proposalSequenceNumber);
            proposalId.add(playerID);

            jsonObject.put("proposal_id", proposalId);

            ArrayList<Integer> playerIds = new ArrayList<>();

            for (JSONObject client: clientList){
                playerIds.add(Integer.parseInt(client.get("player_id").toString()));
            }

            Collections.sort(playerIds);

            for (JSONObject client : clientList){
                int clientId = Integer.parseInt(client.get("player_id").toString());

                if (clientId != playerIds.get(0) && clientId != playerIds.get(1)) {
                    boolean isAccepted = false;

                    for (JSONObject acceptedClient: acceptPromiseList){
                        if (Integer.parseInt(acceptedClient.get("player_id").toString()) == clientId){
                            isAccepted = true;
                            break;
                        }
                    }

                    if (!isAccepted){
                        try {
                            new WerewolfUDPClient(InetAddress.getByName(client.get("address").toString()), Integer.parseInt(client.get("port").toString())).send(jsonObject);
                            System.out.println("Paxos sendPrepareProposal : " + jsonObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

            exec.schedule(new Runnable() {
                @Override
                public void run() {
                    if (state == PAXOS_STATE.PREPARE){
                        System.out.println("Paxos prepare phase timeout !! Resending prepare proposal....");
                        sendPrepareProposal();
                    }
                }
            }, new Random().nextInt((3 - 1) + 1) + 1, TimeUnit.SECONDS);
        }
    }

    void onPreparePromiseReceived(JSONObject message, InetAddress remoteAddress, int remotePort){
        System.out.println("Paxos onPreparePromiseReceived Received : " + message.toString());

        if (role == PAXOS_ROLE.ACCEPTOR){
            Integer[] proposalId = getHighestProposalId();
            Integer[] messageProposalId = new Integer[2];
            messageProposalId[0] = Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString());
            messageProposalId[1] = Integer.parseInt(((JSONArray) message.get("proposal_id")).get(1).toString());

            System.out.println("Message proposal id : " + messageProposalId[0] + " - " + messageProposalId[1]);
            System.out.println("Highest proposal id : " + proposalId[0] + " - " + proposalId[1]);
            System.out.println("Accepted Kpu id : " + getAcceptedKpuID());

            if (messageProposalId[0] > proposalId[0] || (Objects.equals(messageProposalId[0], proposalId[0]) && messageProposalId[1] > proposalId[1])) {
                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "ok");
                    response.put("description", "accepted");
                    response.put("previous_accepted", getAcceptedKpuID());

                    new WerewolfUDPClient(remoteAddress, remotePort).send(response);
                    System.out.println("Paxos onPreparePromiseReceived Sent : " + response);

                    setHighestProposalID(((JSONArray) message.get("proposal_id")).get(0).toString(), ((JSONArray) message.get("proposal_id")).get(1).toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "fail");
                    response.put("description", "rejected");

                    new WerewolfUDPClient(remoteAddress, remotePort).send(response);
                    System.out.println("Paxos onPreparePromiseReceived Sent : " + response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (role == PAXOS_ROLE.LEADER && state == PAXOS_STATE.PREPARE){
            if (message.get("status").toString().equalsIgnoreCase("ok")){
                int previousAcceptedKpuId = Integer.parseInt(message.get("previous_accepted").toString());

                System.out.println("Previous accepted value : " + previousAcceptedKpuId);

                preparePromiseList.add(message);

                if (preparePromiseList.size() + acceptPromiseList.size() > (clientList.size() - 2)/2){
                    int majorityCount = (clientList.size() - 2)/2;
                    int majorityVote = -2;
                    HashMap<String, Integer> counter = new HashMap<>();

//                    Add already accepted promise
                    for (JSONObject acceptedPromise : acceptPromiseList){
                        Integer value = counter.get(acceptedPromise.get("kpu_id").toString());

                        if (value != null){
                            counter.put(acceptedPromise.get("kpu_id").toString(), ++value);

                            if (value > majorityCount){
                                majorityVote = Integer.parseInt(acceptedPromise.get("kpu_id").toString());
                                break;
                            }
                        } else {
                            counter.put(acceptedPromise.get("kpu_id").toString(), 1);
                        }
                    }

//                    Add just received promises
                    for (JSONObject preparePromise : preparePromiseList){
                        Integer value = counter.get(String.valueOf(preparePromise.get("previous_accepted")));

                        if (value != null){
                            counter.put(String.valueOf(preparePromise.get("previous_accepted")), ++value);

                            if (value > majorityCount){
                                majorityVote = Integer.parseInt(preparePromise.get("previous_accepted").toString());
                                break;
                            }
                        } else {
                            counter.put(String.valueOf(preparePromise.get("previous_accepted")), 1);
                        }
                    }

                    System.out.println("Vote result : " + counter);
                    System.out.println("Majority Vote : " + majorityVote);

//                    If majority exist
                    if (majorityVote != -2){
                        if (majorityVote != -1) sendAcceptProposal(majorityVote);
                        else sendAcceptProposal(playerID);
                    } else {
                        int maxValue = -1;
                        int maxKpuId = -2;

                        Iterator it = counter.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry)it.next();

                            String key = pair.getKey().toString();
                            int value = Integer.parseInt(pair.getValue().toString());

                            if (value > maxValue){
                                maxValue = value;

                                if (key.equalsIgnoreCase("-1") && maxKpuId == -2){
                                    maxKpuId = -1;
                                } else {
                                    maxKpuId = Integer.parseInt(key);
                                }
                            }
                        }

                        System.out.println("Max KPU Id : " + maxKpuId);
                        sendAcceptProposal(maxKpuId);
                    }
                }
            }
        }
    }

    void sendAcceptProposal(){
        System.out.println("Paxos sendAcceptProposal : " + getAcceptedKpuID());
        state = PAXOS_STATE.ACCEPT;

        if (role == PAXOS_ROLE.LEADER) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "accept_proposal");

            JSONArray proposalId =  new JSONArray();
            proposalId.add(proposalSequenceNumber);
            proposalId.add(playerID);

            jsonObject.put("proposal_id", proposalId);

            if (getAcceptedKpuID() == -1){
                setAcceptedKpuID(playerID);
            }

            jsonObject.put("kpu_id", getAcceptedKpuID());

            ArrayList<Integer> playerIds = new ArrayList<>();

            for (JSONObject client: clientList){
                playerIds.add(Integer.parseInt(client.get("player_id").toString()));
            }

            Collections.sort(playerIds);

            for (JSONObject client : clientList){
                int clientId = Integer.parseInt(client.get("player_id").toString());

                if (clientId != playerIds.get(0) && clientId != playerIds.get(1)) {
                    boolean isAccepted = false;

                    for (JSONObject acceptedClient: acceptPromiseList){
                        if (Integer.parseInt(acceptedClient.get("player_id").toString()) == clientId){
                            isAccepted = true;
                            break;
                        }
                    }

                    if (!isAccepted){
                        try {
                            new WerewolfUDPClient(InetAddress.getByName(client.get("address").toString()), Integer.parseInt(client.get("port").toString())).send(jsonObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

            exec.schedule(new Runnable() {
                @Override
                public void run() {
                    if (state == PAXOS_STATE.ACCEPT){
                        System.out.println("Paxos accept phase timeout !! Resending prepare proposal....");
                        sendPrepareProposal();
                    }
                }
            }, new Random().nextInt((3 - 1) + 1) + 1, TimeUnit.SECONDS);
        }
    }

    void sendAcceptProposal (int kpuId){
        setAcceptedKpuID(kpuId);
        sendAcceptProposal();
    }

    void onAcceptPromiseReceived(JSONObject message, InetAddress remoteAddress, int remotePort){
        System.out.println("Paxos onAcceptPromiseReceived Received : " + message.toString());

        if (role == PAXOS_ROLE.ACCEPTOR){
            Integer[] proposalId = getHighestProposalId();
            Integer[] messageProposalId = new Integer[2];
            messageProposalId[0] = Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString());
            messageProposalId[1] = Integer.parseInt(((JSONArray) message.get("proposal_id")).get(1).toString());

            Integer messageKpuId = Integer.parseInt(message.get("kpu_id").toString());

            System.out.println("Message proposal id : " + messageProposalId[0] + " - " + messageProposalId[1]);
            System.out.println("Highest proposal id : " + proposalId[0] + " - " + proposalId[1]);

            System.out.println("Received KPU Id : " + messageKpuId);
            System.out.println("Accepted KPU Id : " + getAcceptedKpuID());

//            If never accepted or if accepted value is equal
            if (messageProposalId[0] > proposalId[0] || (Objects.equals(messageProposalId[0], proposalId[0]) && messageProposalId[1] >= proposalId[1])){
                if (getAcceptedKpuID() == -1 || getAcceptedKpuID() == messageKpuId) {
                    try {
                        JSONObject response = new JSONObject();
                        response.put("status", "ok");
                        response.put("description", "accepted");

                        new WerewolfUDPClient(remoteAddress, remotePort).send(response);

                        setHighestProposalID(((JSONArray) message.get("proposal_id")).get(0).toString(), ((JSONArray) message.get("proposal_id")).get(1).toString());
                        setAcceptedKpuID(messageKpuId);

                        System.out.println("ACCEPTED !!! KPU ID : " + getAcceptedKpuID());

                        if (!isCallbackCalled){
                            sendToLearner();

                            onLeaderChosenCallback.onLeaderChosen(getAcceptedKpuID());
                            isCallbackCalled = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        JSONObject response = new JSONObject();
                        response.put("status", "fail");
                        response.put("description", "rejected");

                        new WerewolfUDPClient(remoteAddress, remotePort).send(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "fail");
                    response.put("description", "rejected");

                    new WerewolfUDPClient(remoteAddress, remotePort).send(response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (role == PAXOS_ROLE.LEADER && state == PAXOS_STATE.ACCEPT){
            if (message.get("status").toString().equalsIgnoreCase("ok")) {

                message.put("address", remoteAddress);
                message.put("port", remotePort);

                for (JSONObject client: clientList){
                    if (client.get("address").toString().equalsIgnoreCase(remoteAddress.getHostAddress()) && Integer.parseInt(client.get("port").toString()) == remotePort){
                        message.put("player_id", Integer.parseInt(client.get("player_id").toString()));
                    }
                }

                message.put("kpu_id", getAcceptedKpuID());

                acceptPromiseList.add(message);

                if (acceptPromiseList.size() > (clientList.size() - 2)/2){
                    state = PAXOS_STATE.DONE;
                    System.out.println("LEADER CHOSEN !! : " + getAcceptedKpuID());

                    onLeaderChosenCallback.onLeaderChosen(getAcceptedKpuID());
                }
            }
        }
    }

    void sendToLearner(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "accepted_proposal");
        jsonObject.put("kpu_id", getAcceptedKpuID());
        jsonObject.put("description", "Kpu is selected");

        System.out.println("Paxos sendToLearner : " + jsonObject);

        this.serverSocket.send(jsonObject.toString());
    }

    interface OnLeaderChosenInterface{
        public void onLeaderChosen(int kpuId);
    }

    public void onLeaderChosen(OnLeaderChosenInterface onLeaderChosenCallback){
        this.onLeaderChosenCallback = onLeaderChosenCallback;
    }
}
