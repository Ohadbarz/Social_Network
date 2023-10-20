package bgu.spl.net.api.bidi;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerDataBase {
    private ConcurrentHashMap<String,String> users = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followings = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> loggedInUsers = new ConcurrentHashMap<>();//connectionId
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> awaitMsgForUser = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Short> usersPosts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,Short> usersAge = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> conIdAndName = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> blockedUsers = new ConcurrentHashMap<>();

    private static class ServerDataBaseInst{
        private static ServerDataBase instance = new ServerDataBase();
    }
    private ServerDataBase(){}
    public static synchronized ServerDataBase getInstance(){
        return ServerDataBaseInst.instance;
    }
    public ConcurrentHashMap<String, String> getUsers() {
        return users;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getFollowings() {
        return followings;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getFollowers() {
        return followers;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> getAwaitMsgForUser() {
        return awaitMsgForUser;
    }

    public ConcurrentHashMap<String, Integer> getLoggedInUsers() {
        return loggedInUsers;
    }

    public ConcurrentHashMap<String, Short> getUsersPosts() {
        return usersPosts;
    }

    public ConcurrentHashMap<String, Short> getUsersAge() {
        return usersAge;
    }

    public ConcurrentHashMap<Integer, String> getConIdAndName() {
        return conIdAndName;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getBlockedUsers() {
        return blockedUsers;
    }

    public void increaseOnePostToUser(String userName) {
        short numPosts = this.usersPosts.get(userName);
        numPosts = (short)(numPosts + (short) (1));
        this.getUsersPosts().remove(userName);
        this.getUsersPosts().put(userName,numPosts);

    }

}
