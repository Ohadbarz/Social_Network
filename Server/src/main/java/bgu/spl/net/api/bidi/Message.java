package bgu.spl.net.api.bidi;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class Message {
    private Queue<Short> shorts;
    private Queue<Byte> bytes;
    private Queue<String> strings;


    public Message(Queue<Short> shorts, Queue<Byte> bytes, Queue<String> strings){
        this.shorts = shorts;
        this.bytes = bytes;
        this.strings = strings;
    }
    public Message(){
        shorts = new LinkedList<>();
        bytes = new LinkedList<>();
        strings = new LinkedList<>();
    }


    public Queue<Byte> getBytes() {
        return bytes;
    }

    public Queue<Short> getShorts() {
        return shorts;
    }

    public Queue<String> getStrings() {
        return strings;
    }
    public static void main(String[] args) {
        String s = "aaa"+'\0';
        byte[] arr = s.getBytes(StandardCharsets.UTF_8);
        System.out.println((arr.length));
        for (int i = 0; i < arr.length; i++) {
            System.out.println((int) arr[i]);
        }
    }
}
