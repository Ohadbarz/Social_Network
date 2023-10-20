package bgu.spl.net.api.bidi;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.lang.Short;

public class BidiMessagingProtocolImpl implements BidiMessagingProtocol<Message> {
    private boolean shouldTerminate;
    private ServerDataBase data = ServerDataBase.getInstance();
    private int connID;
    private Connections connections;
    private String username;
    public BidiMessagingProtocolImpl(){
        shouldTerminate = false;
        username = "";
    }

    @Override
    public void start(int connectionId, Connections connections) {
        this.connID = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(Message msg) {
        short opCode = msg.getShorts().peek();
        makeMsg(opCode,msg);
    }
    private void makeMsg(short opCode, Message msg){
        if (opCode == 1) {
            registerMsg(msg);
        } else if (opCode == 2) {
           loginMsg(msg);
        } else if (opCode == 3) {
            logoutMsg(msg);
        } else if (opCode == 4) {
            followMsg(msg);
        } else if (opCode == 5) {
            postMsg(msg);
        } else if (opCode == 6) {
            pmMsg(msg);
        } else if (opCode == 7) {
            logStatMsg(msg);
        } else if (opCode == 8) {
            statsMsg(msg);
        } else { //opCode = 12
            blockMsg(msg);
        }
    }
    private void registerMsg(Message msg){
        username = msg.getStrings().poll();
        if (data.getUsers().containsKey(username)){
            sendError(msg);
        }
        else {
         String password = msg.getStrings().poll();
         String birthDay = msg.getStrings().poll();
         data.getUsers().put(username,password);
         data.getFollowings().put(username,new ConcurrentLinkedQueue<>());
         data.getFollowers().put(username,new ConcurrentLinkedQueue<>());
         data.getAwaitMsgForUser().put(username,new ConcurrentLinkedQueue<>());
         data.getUsersPosts().put(username,(short)0);
         data.getUsersAge().put(username,(short)calculateAge(birthDay));
         data.getBlockedUsers().put(username, new ConcurrentLinkedQueue<>());
         sendACK(msg);
        }
    }
    private void loginMsg(Message msg){
        username =msg.getStrings().poll();
        String password = msg.getStrings().poll();
        if(!data.getUsers().containsKey(username)||!data.getUsers().get(username).equals(password)||data.getLoggedInUsers().containsKey(username)||msg.getBytes().peek()==0)
        {
            sendError(msg);
        }
        else {
            data.getLoggedInUsers().put(username,connID);
            data.getConIdAndName().put(connID,username);
            ConcurrentLinkedQueue<Message> msgToSend = data.getAwaitMsgForUser().get(username);
            while (!msgToSend.isEmpty()){
                connections.send(connID,msgToSend.poll());
            }
            sendACK(msg);
        }
    }
    private void logoutMsg(Message msg){
        if(!data.getConIdAndName().containsKey(connID)){
            sendError(msg);
        }
        else{
            username = data.getConIdAndName().get(connID);
            data.getConIdAndName().remove(connID);
            data.getLoggedInUsers().remove(username);
            sendACK(msg);
            shouldTerminate = true;
            connections.disconnect(connID);
        }
    }
    private void followMsg(Message msg){
        String usernameToFollow = msg.getStrings().peek();
        byte followOrUnfollow = msg.getBytes().peek();
        if(!data.getConIdAndName().containsKey(connID)) {
            sendError(msg);
        }
        else {
            username = data.getConIdAndName().get(connID);
            if (!data.getUsers().containsKey(usernameToFollow)) {
                sendError(msg);
            } else if (followOrUnfollow == 0 && data.getFollowings().get(username).contains(usernameToFollow) || data.getBlockedUsers().get(username).contains(usernameToFollow) || data.getBlockedUsers().get(usernameToFollow).contains(username)) {
                sendError(msg);
            } else if (followOrUnfollow == 1 && !data.getFollowings().get(username).contains(usernameToFollow)) {
                sendError(msg);
            } else {
                if (followOrUnfollow == 0) {
                    data.getFollowings().get(username).add(usernameToFollow);
                    data.getFollowers().get(usernameToFollow).add(username);
                } else {
                    data.getFollowings().get(username).remove(usernameToFollow);
                    data.getFollowers().get(usernameToFollow).remove(username);
                }
                sendACK(msg);
            }
        }
    }
    private void postMsg(Message msg) {
        if (!data.getConIdAndName().containsKey(connID)) {
            sendError(msg);
        } else {
            boolean changed = false;
            username = data.getConIdAndName().get(connID);
            ConcurrentLinkedQueue<String> followers = data.getFollowers().get(username);
            String content = msg.getStrings().peek();
            byte[] post = shortToBytes((short) 1);
            msg.getBytes().add(post[1]);
            while (content.contains("@")) {
                int indexOfTag = content.indexOf("@");
                content = content.substring(indexOfTag + 1);
                String taggedUser = "";
                if (content.contains(" ")) {
                    int indexOfSpace = content.indexOf(" ");
                    taggedUser = content.substring(0, indexOfSpace);
                    content = content.substring(indexOfSpace + 1);
                } else {
                    taggedUser = content;
                }
                if (data.getUsers().containsKey(taggedUser)) {
                    if (!followers.contains(taggedUser)&&!data.getBlockedUsers().get(username).contains(taggedUser)&&!data.getBlockedUsers().get(taggedUser).contains(username)) {
                        Short op = msg.getShorts().peek();
                        createNotification(username,msg);
                        changed = true;
                        msg.getShorts().add(op);
                        if (data.getLoggedInUsers().containsKey(taggedUser)) {
                            connections.send(data.getLoggedInUsers().get(taggedUser), msg);
                        } else {
                            data.getAwaitMsgForUser().get(taggedUser).add(msg);
                        }
                    }
                }
            }
            if(!changed) {
                Short op = msg.getShorts().peek();
                createNotification(username, msg);
                msg.getShorts().add(op);
            }
            for (String follower : followers) {
                if (data.getLoggedInUsers().containsKey(follower)) {
                    connections.send(data.getLoggedInUsers().get(follower), msg);
                } else {

                    data.getAwaitMsgForUser().get(follower).add(msg);
                }
            }
            data.increaseOnePostToUser(username);
            Message msg2 = new Message();
            msg2.getShorts().add((short) 5);
            sendACK(msg2);
        }
    }
    private void sendACK(Message msg){
        short sourceOpCode = msg.getShorts().poll();
        short ackOpCode = 10;
        msg.getShorts().add(ackOpCode);
        msg.getShorts().add(sourceOpCode);
        connections.send(connID, msg);
    }
    private void pmMsg(Message msg){
        if (!data.getConIdAndName().containsKey(connID)) {
            sendError(msg);
        }
        else{
            username = data.getConIdAndName().get(connID);
            boolean changed = false;
            byte[] pm = shortToBytes((short)0);
            msg.getBytes().add(pm[1]);
            ConcurrentLinkedQueue followings = data.getFollowings().get(username);
            String userToSend = msg.getStrings().poll();
            if(!data.getUsers().containsKey(userToSend)||!followings.contains(userToSend)){
                sendError(msg);
            }
            else {
                String content = msg.getStrings().poll();
                LinkedList<String> toFilter = new LinkedList<>();
                toFilter.add("use drugs");
                toFilter.add("only Trump");
                toFilter.add("idiot");
                toFilter.add("kill");
                while (!toFilter.isEmpty()){
                    content = content.replaceAll(toFilter.remove(),"<filtered>");
                }
                msg.getStrings().add(content);
                Short op = msg.getShorts().peek();
                createNotification(username,msg);
                msg.getShorts().add(op);
                if(data.getLoggedInUsers().containsKey(userToSend)){
                    int userToSendId = data.getLoggedInUsers().get(userToSend);
                    connections.send(userToSendId,msg);
                }
                else {
                    data.getAwaitMsgForUser().get(userToSend).add(msg);
                }
                Message msg2 = new Message();
                msg2.getShorts().add((short) 6);
                sendACK(msg2);

            }
        }
    }
    private void logStatMsg(Message msg){
        if(!data.getConIdAndName().containsKey(connID)){
            sendError(msg);
        }
        else {
            username = data.getConIdAndName().get(connID);
            Iterator iterator = data.getLoggedInUsers().keySet().iterator();
            while (iterator.hasNext()){
               String user = iterator.next().toString();
               if(!data.getBlockedUsers().get(username).contains(user)&&!data.getBlockedUsers().get(user).contains(username)) {
                   byte[] age = shortToBytes(data.getUsersAge().get(user));
                   msg.getBytes().add(age[0]);
                   msg.getBytes().add(age[1]);
                   byte[] numPosts = shortToBytes(data.getUsersPosts().get(user));
                   msg.getBytes().add(numPosts[0]);
                   msg.getBytes().add(numPosts[1]);
                   byte[] numFollower =  shortToBytes((short) data.getFollowers().get(user).size());
                   msg.getBytes().add(numFollower[0]);
                   msg.getBytes().add(numFollower[1]);
                   byte[] numFollowing = shortToBytes((short) data.getFollowings().get(user).size());
                   msg.getBytes().add(numFollowing[0]);
                   msg.getBytes().add(numFollowing[1]);
                   short op = msg.getShorts().peek();
                   sendACK(msg);
                   msg.getShorts().add(op);
                   while (!msg.getBytes().isEmpty()) {
                       msg.getBytes().poll();
                   }
               }
            }
        }
    }
    private void statsMsg(Message msg){
        if(!data.getConIdAndName().containsKey(connID)){
            sendError(msg);
        }
        else {
            username = data.getConIdAndName().get(connID);
            String users = msg.getStrings().poll();
            while (users.length()>0){
                int index = users.indexOf('|');
                String user = users.substring(0,index);
                users = users.substring(index+1);
                if(!data.getUsers().containsKey(user)||data.getBlockedUsers().get(username).contains(user)||data.getBlockedUsers().get(user).contains(username)){
                    sendError(msg);
                }
                else {
                    byte[] age = shortToBytes(data.getUsersAge().get(user));
                    msg.getBytes().add(age[0]);
                    msg.getBytes().add(age[1]);
                    byte[] numPosts = shortToBytes(data.getUsersPosts().get(user));
                    msg.getBytes().add(numPosts[0]);
                    msg.getBytes().add(numPosts[1]);
                    byte[] numFollower = shortToBytes((short) data.getFollowers().get(user).size());
                    msg.getBytes().add(numFollower[0]);
                    msg.getBytes().add(numFollower[1]);
                    byte[] numFollowing = shortToBytes((short) data.getFollowings().get(user).size());
                    msg.getBytes().add(numFollowing[0]);
                    msg.getBytes().add(numFollowing[1]);
                    short op = msg.getShorts().peek();
                    sendACK(msg);
                    msg.getShorts().add(op);
                    while (!msg.getBytes().isEmpty()) {
                        msg.getBytes().poll();
                    }
                }
            }
        }
    }
    private void blockMsg(Message msg){
        if(!data.getConIdAndName().containsKey(connID)){
            sendError(msg);
        }
        else {
            username = data.getConIdAndName().get(connID);
            String userToBlock = msg.getStrings().poll();
            if(data.getBlockedUsers().get(username).contains(userToBlock)){
                sendError(msg);
            }
            else if(!data.getUsers().containsKey(userToBlock)){

                sendError(msg);
            }
            else{
                data.getBlockedUsers().get(username).add(userToBlock);
                if(data.getFollowings().get(username).contains(userToBlock)){
                    data.getFollowings().get(username).remove(userToBlock);
                    data.getFollowers().get(userToBlock).remove(username);
                }
                if(data.getFollowers().get(username).contains(userToBlock)){
                    data.getFollowers().get(username).remove(userToBlock);
                    data.getFollowings().get(userToBlock).remove(username);
                }
                sendACK(msg);
            }
        }
    }
    private void sendError(Message msg) {
        short sourceOpCode = msg.getShorts().poll();
        short errOpCode = 11;
        msg.getShorts().add(errOpCode);
        msg.getShorts().add(sourceOpCode);
        connections.send(connID, msg);
    }


    private void createNotification(String username, Message msg) {
        msg.getShorts().poll();
        msg.getShorts().add((short)9);
        String content = msg.getStrings().poll();
        msg.getStrings().add(username);
        msg.getStrings().add(content);
//        msg.setSendingUserName(data.getConIdAndName().get(connID));
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    private short calculateAge(String birthDay){
        int index = birthDay.indexOf('-');
        String day = birthDay.substring(0,index);
        birthDay = birthDay.substring(index+1);
        index = birthDay.indexOf('-');
        String month = birthDay.substring(0,index);
        String year = birthDay.substring(index+1);
        LocalDate end = LocalDate.now();
        LocalDate birth = LocalDate.of(Integer.valueOf(year),Integer.valueOf(month),Integer.valueOf(day));
        short age = (short) ChronoUnit.YEARS.between(birth,end);
        return age;
    }
    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }




}
