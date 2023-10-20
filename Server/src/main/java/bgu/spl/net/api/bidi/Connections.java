package bgu.spl.net.api.bidi;

public interface Connections<T> {
    boolean send(int connId, T msg);
    void  broadcast(T msg);
    void disconnect(int connId);

}
