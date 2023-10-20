package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.ConnectionHandler;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionsHandler;
    public ConnectionsImpl(){
        this.connectionsHandler = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean send(int connId, T msg) {
        if (connectionsHandler.containsKey(connId)) {
            connectionsHandler.get(connId).send(msg);
            return true;
        }
        return false;
    }


    @Override
    public void broadcast(T msg) {
        for(ConnectionHandler<T> ch: connectionsHandler.values()){
            ch.send(msg);
        }
    }

    @Override
    public void disconnect(int connId) {
        connectionsHandler.remove(connId);
    }
    public void add(int conID, ConnectionHandler<T> handler){
        connectionsHandler.put(conID, handler);
    }
}
