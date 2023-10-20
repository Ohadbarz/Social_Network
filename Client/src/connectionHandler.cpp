
#include <iomanip>
#include "../include/connectionHandler.h"
#include "../src/EncodeDecode.cpp"
using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
 
ConnectionHandler::ConnectionHandler(string host, short port): host_(host), port_(port), io_service_(), socket_(io_service_){}
    
ConnectionHandler::~ConnectionHandler() {
    close();
}

std::string shortToChar(short opCode){
    std::string res = "";
    res = res + (char)(opCode>>8) + (char)(opCode);
    return res;
}
void shortToBytes(short num, char* bytesArr)
{
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}
std::string getTime(){
    auto t = std::time(nullptr);
    auto tm = *std::localtime(&t);
    std::ostringstream stream;
    stream<<std::put_time(&tm, "%d-%m-%Y %H-%M" );
    auto str = stream.str();
    return str;
}


std::string sendRegister(std::string msg) {
    std::string output = "";
    output += (char)0;
    output += (char)1;
    std::string username = msg.substr(0, msg.find_first_of(' '));
    msg = msg.substr(msg.find_first_of(' ')+1);
    std::string password = msg.substr(0, msg.find_first_of(' '));
    msg = msg.substr(msg.find_first_of(' ')+1);
    std::string birthday = msg;
    output = output + username + '\0' + password + '\0' + birthday + '\0';
    return output;
}
std::string sendLogin(std::string msg){
    std::string output = "";
    short captcha;
    output += (char)0;
    output += (char)2;
    std::string username = msg.substr(0, msg.find_first_of(' '));
    msg = msg.substr(msg.find_first_of(' ')+1);
    std::string password = msg.substr(0, msg.find_first_of(' '));
    msg = msg.substr(msg.find_first_of(' ')+1);
    if(msg.at(0)=='0'){
        captcha = 0;
    }
    else{
        captcha = 1;
    }
    output = output + username + '\0' + password + '\0' + (char)(captcha);
    return output;
}
std::string sendLogout(std::string msg) {
    std::string output = "";
    output += (char)0;
    output += (char)3;
    return output;
}
std::string sendFollow(std::string msg){
    std::string output = "";
    output += (char)0;
    output += (char)4;
    short followOrUnfollow;
    if(msg.at(0)=='0'){
        followOrUnfollow = 0;
    }
    else{
        followOrUnfollow = 1;
    }
    msg = msg.substr(msg.find_first_of(' ')+1);
    std::string username = msg;
    output += (char)followOrUnfollow + username+'\0';
    return output;
}
std::string sendPost(std::string msg){
    std::string output = "";
    output += (char)0;
    output += (char)5;
    output += msg + '\0';
    return output;
}
std::string sendPm(std::string msg){
    std::string output = "";
    output += (char)0;
    output += (char)6;
    std::string username = msg.substr(0, msg.find_first_of(' '));
    msg = msg.substr(msg.find_first_of(' ')+1);
    std::string content ="";
    content = msg;
    output+= username + '\0' + content +'\0'+ getTime() + '\0';
    return output;
}
std::string sendLogStat(std::string msg) {
    std::string output = "";
    short opcode = 7;
    output += shortToChar(opcode);
    return output;
}
std::string sendStat(std::string msg){
    std::string output = "";
    output += (char)0;
    output += (char)8;
    output+= msg;
    return output + '\0';
}
std::string sendBlock(std::string msg){
    std::string output = "";
    output += (char)0;
    output += (char)9;
    output+= msg;
    return output + '\0';
}
    std::string encode(std::string msg) {
        std::string act = msg.substr(0, msg.find_first_of(' '));
        msg = msg.substr(msg.find_first_of(' ') + 1);
        if (act == "REGISTER") {
            return sendRegister(msg);
        } else if (act == "LOGIN") {
            return sendLogin(msg);
        } else if (act == "LOGOUT") {
            return sendLogout(msg);
        } else if (act == "FOLLOW") {
            return sendFollow(msg);
        } else if (act == "POST") {
            return sendPost(msg);
        } else if (act == "PM") {
            return sendPm(msg);
        } else if (act == "LOGSTAT") {
            return sendLogStat(msg);
        } else if (act == "STAT") {
            return sendStat(msg);
        } else if (act == "BLOCK") {
            return sendBlock(msg);
        }
        else{
            return "";
        }
    }
 
bool ConnectionHandler::connect() {
    std::cout << "Starting connect to " 
        << host_ << ":" << port_ << std::endl;
    try {
		tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
		boost::system::error_code error;
		socket_.connect(endpoint, error);
		if (error)
			throw boost::system::system_error(error);
    }
    catch (std::exception& e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
 
bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp ) {
			tmp += socket_.read_some(boost::asio::buffer(bytes+tmp, bytesToRead-tmp), error);			
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp ) {
			tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
bool ConnectionHandler::getFrameAscii(std::string& frame, char delimiter) {
    char ch;
    // Stop when we encounter the null character.
    // Notice that the null character is not appended to the frame string.
    try {
        do{
            getBytes(&ch, 1);
            if (ch != delimiter) {
                frame.append(1, ch);
            }
        }while (delimiter != ch);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendFrameAscii(const std::string& frame, char delimiter) {
    bool result=sendBytes(frame.c_str(),frame.length());
    if(!result) return false;
    return sendBytes(&delimiter,1);
}

// Close down the connection properly.
void ConnectionHandler::close() {
    try {
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}
short bytesToShort(char* bytesArr){
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}
std::string ConnectionHandler::decode(std::string msg){
    char opBytes[2];
    getBytes(opBytes, 2);
    short  opCode = bytesToShort(opBytes);
    if(opCode == 11){
        char sourceOP[2];
        getBytes(sourceOP, 2);
        msg += "ERROR " + std::to_string(bytesToShort(sourceOP));
    }
    else if(opCode == 9){
        msg += "NOTIFICATION ";
        char postOrPm[1];
        getBytes(postOrPm,1);
        short check = bytesToShort(postOrPm);
        if(check== 0){
            msg += "PM ";
        }
        else{
            msg+= "Public ";
        }
        getFrameAscii(msg, '\0');
        msg += " ";
        getFrameAscii(msg, '\0');
    }
    else if(opCode == 10){
        msg += "ACK ";
        char sourceOP[2];
        getBytes(sourceOP, 2);
        short op = bytesToShort(sourceOP);
        msg += std::to_string(op);
            if(op == 4){
                msg += " ";
                char followOrUnfollow[1];
                getBytes(followOrUnfollow,1);
                if(followOrUnfollow[0]== '0'){
                    msg += "0 ";
                }
                else{
                    msg += "1 ";
                }
            }
            else if (op==7 || op==8){
                msg += " ";
                char age[2];
                getBytes(age, 2);
                short age1 = bytesToShort(age);
                msg += std::to_string(age1);
                msg += " ";
                char NumPosts[2];
                getBytes(NumPosts, 2);
                short post = bytesToShort(NumPosts);
                msg += std::to_string(post);
                msg += " ";
                char NumFollowers[2];
                getBytes(NumFollowers, 2);
                short Followers = bytesToShort(NumFollowers);
                msg += std::to_string(Followers);
                msg += " ";
                char NumFollowings[2];
                getBytes(NumFollowings, 2);
                short Followings = bytesToShort(NumFollowings);
                msg += std::to_string(Followings);
            }
    }
    return msg;
}

bool ConnectionHandler::getLine(std::string& line) {
    line = decode(line);
    return getFrameAscii(line, ';');
}

bool ConnectionHandler::sendLine(std::string& line) {
    std::string newLine = "";
    newLine = encode(line);
    return sendFrameAscii(newLine, ';');
}
