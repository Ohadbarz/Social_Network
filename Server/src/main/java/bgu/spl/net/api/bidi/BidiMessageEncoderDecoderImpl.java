package bgu.spl.net.api.bidi;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class BidiMessageEncoderDecoderImpl implements MessageEncoderDecoder<Message> {
    private byte[] bytes = new byte[1 << 10];
    private int len = 0;
    private short opCode = 0;
    private Queue<Short> shortsPart;//for opCode
    private Queue<Byte> bytesPart;//for cases such follow/unfollow
    private Queue<String> stringsPart;//for cases such username-password-birthday
    private int zerosCounter = 0;


    public Message decodeNextByte(byte nextByte) {
        Message msg = null;
        if (nextByte != ';') {
            pushByte(nextByte);
        }
        //check if the opcode could be one digit
        else {
            opCode = bytesToShort(bytes);
            shortsPart = new LinkedBlockingQueue<>();
            bytesPart = new LinkedBlockingQueue<>();
            stringsPart = new LinkedBlockingQueue<>();
            shortsPart.add(opCode);
            msg = makeDecMsg(opCode, nextByte);
        }
        if (msg != null) {
            opCode = 0;
            len = 0;
            zerosCounter = 0;
        }
        return msg;
    }

    private Message makeDecMsg(short op, byte nextByte) {
        if (op == 1) {
            return registerMsg(nextByte);
        } else if (op == 2) {
            return loginMsg(nextByte);
        } else if (op == 3) {
            return logoutMsg(nextByte);
        } else if (op == 4) {
            return followMsg(nextByte);
        } else if (op == 5) {
            return postMsg(nextByte);
        } else if (op == 6) {
            return pmMsg(nextByte);
        } else if (op == 7) {
            return logStatMsg(nextByte);
        } else if (op == 8) {
            return statsMsg(nextByte);
        } else { //op = 12
            return blockMsg(nextByte);
        }
    }


    public byte[] encode(Message msg) {
        short opCode = msg.getShorts().peek();
        if (opCode == 9) {
            return notificationMsg(msg);
        } else if (opCode == 10) {
            return ackMsg(msg);
        } else {//opCode = 11
            return errMsg(msg);
        }
    }
    private byte[] notificationMsg(Message msg) {
        short opCode = msg.getShorts().poll();
        byte[] ByteInOpCode = shortToBytes(opCode);
        LinkedList<Byte> bytesToEnc = new LinkedList<Byte>();
        bytesToEnc.add(ByteInOpCode[0]);
        bytesToEnc.add(ByteInOpCode[1]);

        bytesToEnc.add(msg.getBytes().poll());//1 - post, 0 - PM -- TO UPDATE IN PROTOCOL!!!!!!!!!!1
        byte[] sendingUserName = msg.getStrings().poll().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < sendingUserName.length; i++) {
            bytesToEnc.add(sendingUserName[i]);
        }
        bytesToEnc.add((byte) ('\0'));
        byte[] content = msg.getStrings().peek().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < content.length; i++) {
            bytesToEnc.add(content[i]);
        }
        bytesToEnc.add((byte) ('\0'));
        String end =";";
        bytesToEnc.add((byte)(';'));
        byte[] encoded = new byte[bytesToEnc.size()];
        for (int i = 0; i < encoded.length; i++) {
            encoded[i] = bytesToEnc.poll();
        }
        return encoded;
    }

    private byte[] errMsg(Message msg) {
        short opCode = msg.getShorts().poll();//opCode of error
        byte[] ByteInOpCode = shortToBytes(opCode);
        LinkedList<Byte> bytesToEnc = new LinkedList<Byte>();
        bytesToEnc.add(ByteInOpCode[0]);
        bytesToEnc.add(ByteInOpCode[1]);
        opCode = msg.getShorts().poll();//opCode of source message
        ByteInOpCode = shortToBytes(opCode);
        bytesToEnc.add(ByteInOpCode[0]);
        bytesToEnc.add(ByteInOpCode[1]);
        bytesToEnc.add((byte)(';'));
        byte[] encoded = new byte[bytesToEnc.size()];
        for (int i = 0; i < encoded.length; i++) {
            encoded[i] = bytesToEnc.poll();
        }
        return encoded;
    }


    private byte[] ackMsg(Message msg) {
        short opCode = msg.getShorts().poll();//opCode of ack
        byte[] ByteInOpCode = shortToBytes(opCode);
        LinkedList<Byte> bytesToEnc = new LinkedList<Byte>();
        bytesToEnc.add(ByteInOpCode[0]);
        bytesToEnc.add(ByteInOpCode[1]);
        opCode = msg.getShorts().poll();//opCode of source message
        ByteInOpCode = shortToBytes(opCode);
        bytesToEnc.add(ByteInOpCode[0]);
        bytesToEnc.add(ByteInOpCode[1]);
        if (opCode == 4) {
            return optional4follow(bytesToEnc, msg);
        } else if ((opCode == 7)||(opCode==8)) {
            return optional7or8(bytesToEnc, msg);
        } else {
            bytesToEnc.add((byte)(';'));
            byte[] encoded = new byte[bytesToEnc.size()];
            for (int i = 0; i < encoded.length; i++) {
                encoded[i] = bytesToEnc.poll();
            }
            return encoded;
        }
    }

    private byte[] optional4follow(LinkedList<Byte> bytesToEnc, Message msg) {
        bytesToEnc.add(msg.getBytes().poll());
        byte[] userName = msg.getStrings().poll().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < userName.length; i++) {
            bytesToEnc.add(userName[i]);
        }
        bytesToEnc.add((byte)(';'));
        byte[] encoded = new byte[bytesToEnc.size()];
        for (int i = 0; i < encoded.length; i++) {
            encoded[i] = bytesToEnc.poll();
        }
        return encoded;
    }
    private byte[] optional7or8(LinkedList<Byte> bytesToEnc, Message msg){
        while (!msg.getBytes().isEmpty()){
            bytesToEnc.add(msg.getBytes().poll());
        }
        bytesToEnc.add((byte)(';'));
        byte[] encoded = new byte[bytesToEnc.size()];
        for (int i = 0; i < encoded.length; i++) {
            encoded[i] = bytesToEnc.poll();
        }
        return encoded;
    }

    public short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }
    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }
//    private short calculateOpCode(byte[] arr) {
//        return bytesToShort(arr);
//    }


    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len] = nextByte;
        len++;
    }

    private Message registerMsg(byte nextByte) {
        String registerMsg = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
        //check if the opcode could be one digit
        int indexOfZero = registerMsg.indexOf('\0');
        String userName = registerMsg.substring(0, indexOfZero);
        registerMsg = registerMsg.substring(indexOfZero + 1);//update the message to continue parsing
        indexOfZero = registerMsg.indexOf('\0');
        String password = registerMsg.substring(0, indexOfZero);
        registerMsg = registerMsg.substring(indexOfZero + 1);
        indexOfZero = registerMsg.indexOf('\0');
        String birthDay = registerMsg.substring(0, indexOfZero);
        stringsPart.add(userName);
        stringsPart.add(password);
        stringsPart.add(birthDay);
        return new Message(shortsPart, bytesPart, stringsPart);
    }

    private Message loginMsg(byte nextByte) {
        String loginMsg = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
        int indexOfZero = loginMsg.indexOf('\0');
        String userName = loginMsg.substring(0, indexOfZero);
        loginMsg = loginMsg.substring(indexOfZero + 1);//update the message to continue parsing
        indexOfZero = loginMsg.indexOf('\0');
        String password = loginMsg.substring(0, indexOfZero);
        loginMsg = loginMsg.substring(indexOfZero + 1);
        byte captcha = loginMsg.getBytes(StandardCharsets.UTF_8)[0];
        stringsPart.add(userName);
        stringsPart.add(password);
        bytesPart.add(captcha);
        return new Message(shortsPart, bytesPart, stringsPart);
    }

    private Message logoutMsg(byte nextByte) {
        return new Message(shortsPart, bytesPart, stringsPart);
    }

    private Message followMsg(byte nextByte) {
        String followMsg = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
        byte followOrUnFollow = followMsg.getBytes(StandardCharsets.UTF_8)[0];
        followMsg = followMsg.substring(1);
        int indexOfZero = followMsg.indexOf('\0');
        String userName = followMsg.substring(0,indexOfZero);
        stringsPart.add(userName);
        bytesPart.add(followOrUnFollow);
        return new Message(shortsPart, bytesPart, stringsPart);
    }

    private Message postMsg(byte nextByte) {
        String postMsg = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
        int index = postMsg.indexOf('\0');
        String content = postMsg.substring(0, index);
        stringsPart.add(content);
        return new Message(shortsPart, bytesPart, stringsPart);
    }

    private Message pmMsg(byte nextByte) {
        String pmMsg = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
        //check if the opcode could be one digit
        int indexOfZero = pmMsg.indexOf('\0');
        String userName = pmMsg.substring(0, indexOfZero);
        pmMsg = pmMsg.substring(indexOfZero + 1);//update the message to continue parsing
        indexOfZero = pmMsg.indexOf('\0');
        String content = pmMsg.substring(0, indexOfZero);
        stringsPart.add(userName);
        stringsPart.add(content);
        return new Message(shortsPart, bytesPart, stringsPart);
    }

    private Message logStatMsg(byte nextByte) {
        return new Message(shortsPart, bytesPart, stringsPart);
    }

    private Message statsMsg(byte nextByte) {
        String statsMsg = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
        int index = statsMsg.indexOf('\0');
        String listUserNames = statsMsg.substring(0, index);
        stringsPart.add(listUserNames);
        return new Message(shortsPart, bytesPart, stringsPart);
    }


    private Message blockMsg(byte nextByte) {
        String blockMsg = new String(bytes, 2, len - 2, StandardCharsets.UTF_8);
        int index = blockMsg.indexOf('\0');
        String userName = blockMsg.substring(0, index);
        stringsPart.add(userName);
        return new Message(shortsPart, bytesPart, stringsPart);
    }

}

