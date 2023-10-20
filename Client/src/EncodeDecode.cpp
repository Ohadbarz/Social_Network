//#include "../include/connectionHandler.h"
//
////
//// Created by alon on 07/01/2022.
////
//class EncodeDecode{
//private:
//    ConnectionHandler &con;
//    bool & shouldTerminate;
//public:
//    EncodeDecode(ConnectionHandler &con, bool &shouldTerminate) : con(con), shouldTerminate(shouldTerminate) {}

//    std::string encode(std::string msg) {
//        std::string act = msg.substr(0, msg.find_first_of(' '));
//        msg = msg.substr(msg.find_first_of(' ') + 1);
//        if (act == "REGISTER") {
//           return sendRegister(msg);
//        } else if (act == "LOGIN") {
//            return sendLogin(msg);
//        } else if (act == "LOGOUT") {
//            return sendLogout(msg);
//        } else if (act == "FOLLOW") {
//            return sendFollow(msg);
//        } else if (act == "POST") {
//            return sendPost(msg);
//        } else if (act == "PM") {
//            return sendPm(msg);
//        } else if (act == "LOGSTAT") {
//            return sendLogStat(msg);
//        } else if (act == "STAT") {
//            return sendStat(msg);
//        } else if (act == "BLOCK") {
//            return sendBlock(msg);
//        } else {
//            std::cout << "illegal act" << std::endl;
//            return "";
//        }
//    }
//    std::string shortToChar(short opCode){
//        std::string res = "";
//        res = res + (char)(opCode>>8) + (char)(opCode);
//        return res;
//    }
//
//
//    std::string sendRegister(std::string msg) {
//             std::string output = "";
//             short opcode = 1;
//             output += shortToChar(opcode);
//             std::string username = msg.substr(0, msg.find_first_of(' '));
//             msg = msg.substr(msg.find_first_of(' ')+1);
//             std::string password = msg.substr(0, msg.find_first_of(' '));
//             msg = msg.substr(msg.find_first_of(' ')+1);
//             std::string birthday = msg;
//             output = output + username + '\0' + password + '\0' + birthday + '\0';
//             return output;
//        }
//        std::string sendLogin(std::string msg){
//            std::string output = "";
//            short opcode = 2;
//            short captcha;
//            output += shortToChar(opcode);
//            std::string username = msg.substr(0, msg.find_first_of(' '));
//            msg = msg.substr(msg.find_first_of(' ')+1);
//            std::string password = msg.substr(0, msg.find_first_of(' '));
//            msg = msg.substr(msg.find_first_of(' ')+1);
//            if(msg.at(0)=='0'){
//                captcha = 0;
//            }
//            else{
//                captcha = 1;
//            }
//            output = output + username + '\0' + password + '\0' + (char)(captcha);
//            return output;
//        }
//        std::string sendLogout(std::string msg) {
//            std::string output = "";
//            short opcode = 3;
//            output += shortToChar(opcode);
//            return output;
//        }
//        std::string sendFollow(std::string msg){
//        std::string output = "";
//        short opCode = 4;
//        output += output + shortToChar(opCode);
//        short followOrUnfollow;
//        if(msg.at(0)=='0'){
//            followOrUnfollow = 0;
//        }
//        else{
//            followOrUnfollow = 1;
//        }
//        msg = msg.substr(msg.find_first_of(' ')+1);
//        std::string username = msg;
//        output += (char)followOrUnfollow + username+'\0';
//        return output;
//    }
//    std::string sendPost(std::string msg){
//        std::string output = "";
//        short opcode = 5;
//        output += shortToChar(opcode);
//        output += msg + '\0';
//        return output;
//    }
//    std::string sendPm(std::string msg){
//        std::string output = "";
//        short opcode = 6;
//        output += shortToChar(opcode);
//        std::string username = msg.substr(0, msg.find_first_of(' '));
//        msg = msg.substr(msg.find_first_of(' ')+1);
//        std::string content ="";
//        while(msg.find_first_of(' ')!=-1){
//            content += msg.substr(0, msg.find_first_of(' ')+1);
//            msg = msg.substr(msg.find_first_of(' ')+ 1);
//        }
//        std::string date = msg;
//        output+= username + '\0' + content +'\0' + date + '\0';
//    }
//    std::string sendLogStat(std::string msg) {
//        std::string output = "";
//        short opcode = 7;
//        output += shortToChar(opcode);
//        return output;
//    }
//    std::string sendStat(std::string msg){
//        std::string output = "";
//        short opcode = 8;
//        output += shortToChar(opcode);
//        output+= msg;
//        return output + '\0';
//    }
//    std::string sendBlock(std::string msg){
//        std::string output = "";
//        short opcode = 12;
//        output += shortToChar(opcode);
//        output+= msg;
//        return output + '\0';
//    }
//    void write(){
//        while(!shouldTerminate){
//            const short bufsize = 1024;
//            char buf[bufsize];
//            std::cin.getline(buf,bufsize);
//            std::string command(buf);
//            std::string toServer = encode(command);
//            if(toServer!=""){
//                if(!con.sendLine(toServer)){
//                    std::cout << "Disconnected. Exiting...\n" << std::endl;
//                    shouldTerminate = true;
//                }
//                else if(command == "LOGOUT"){
//                    shouldTerminate = true;
//                    return;
//                }
//            }
//        }
//    }
//};
