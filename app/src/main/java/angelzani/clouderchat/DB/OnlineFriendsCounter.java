package angelzani.clouderchat.DB;

import java.util.ArrayList;

public class OnlineFriendsCounter {

    private ArrayList<String> counterUIDs = new ArrayList<>();

    public int getOnlineCount(String userUID, boolean isOnline){
        final int size = counterUIDs.size();
        boolean isAdded = false;
        int index = 0;
        for(int i=0; i<size; i++){
            if(counterUIDs.get(i).equals(userUID)){
                isAdded = true;
                index = i;
                break; // знаем, че е добавен, няма смисъл да продължаваме
            }
        }

        if(isAdded && !isOnline){
            counterUIDs.remove(index);
        }
        else if(!isAdded && isOnline) {
            counterUIDs.add(userUID);
        }

        return counterUIDs.size();
    }

}
