package angelzani.clouderchat.DB;

import java.util.HashMap;

public class OnlineFriendsCounter {

    private HashMap<String, Boolean> counterUIDs = new HashMap<>();

    public int getOnlineCount(String userUID, boolean isOnline){
        if(counterUIDs.containsKey(userUID) && !isOnline){
            counterUIDs.remove(userUID);
        }
        else if(!counterUIDs.containsKey(userUID) && isOnline) {
            counterUIDs.put(userUID, true);
        }
        return counterUIDs.size();
    }

}