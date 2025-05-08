import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

//定义一个名为TupleClient的公共类。可使用socket连接到服务器。（Define a public class called TupleClient. You can use a socket to connect to the server.）
public class TupleClient {
    //声明一个值为10000毫秒的，类型为int的常量TIMEOUT，防止无限等待（Declare a constant TIMEOUT of type int with a value of 10,000 milliseconds to prevent infinite waiting.）
    private static int TIMEOUT = 10000;

    //主程序入口（Main program entry）
    public static void main(String[] args) {
        //检查命令行参数数量是否为程序所需要的三个参数：主机名、端口号和文件名（Check whether the number of command line arguments is the three required parameters for the program: hostname, port number, and filename.）
        if (args.length != 3) {
            System.out.println("Need hostname, port, and file name!");
            return;//参数不正确则退出，不再执行接下来的代码（If the parameters are incorrect, exit and do not execute the following code）
        }

        //赋值（evaluation）
        String hostname = args[0];
        int port;  //储存端口号
        //尝试将String类型转换到int类型（Try to convert the String type to the int type）
        try {
            port = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Port number is wrong!");
            return;
        }

        //将第三个命令行参数args[2]赋值给String变量filename（Assign the third command line parameter, args[2], to the String variable filename）
        String filename = args[2];

        //声明多个变量用于网络通信和文件读取并统一初始化为null（Declare multiple variables for network communication and file reading and initialize them uniformly to null）
        Socket socket = null;//与服务器建立联系(Connect to the server)
        PrintWriter out = null;//向服务器发送数据(Send data to the server)
        BufferedReader in =null;//读取服务器响应(Read the server response)
        BufferedReader filReader = null;//读取本地文件(Read local files)

        try {
            //创建新的Socket对象，尝试连接到指定的hostname和port,这会建立与服务器的TCP连接
            //Create a new Socket object and try to connect to the specified hostname and port, which will establish a TCP connection with the server.
            socket = new Socket(hostname, port);
            socket.setSoTimeout(TIMEOUT); //若超时（10秒）则抛出错误。(If the timeout (10 seconds) is exceeded, an error is thrown)
            //创建变量们的对象以方便下一步的实现
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            filReader = new BufferedReader(new FileReader(filename));

            String line;
            while ((line = filReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                //处理当前行的命令并获取服务器响应(Process the command for the current line and get the server response)
                String result = processRequest(line,out,in);
                //print
                System.out.println("Read line: " + line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Can't find file: " + filename);
            //System.out.println("Connected to server!");
        } catch (Exception e) {
            System.out.println("Can't connect to server: " + e.getMessage());
        } finally {  //开始执行finally块，无论try块是否抛出异常，此块都会执行，可以清理资源（虽然我觉得有点没有必要）
            //Start executing the finally block, which will be executed regardless of whether the try block throws an exception or not, and can clean up resources (though I don't think it's necessary)
            //if (socket != null) {
            //在finally块中关闭所有打开的资源(close)
                try {
                if (filReader != null) filReader.close();
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
                    //socket.close();
                } catch (Exception e) {
                    System.out.println("Error closing stuff!");
                }
            }

        }

        private static final int MAX_KEY_VALUE_LENGTH = 999;
        private static final int MAX_TUPLE_SIZE = 970;

        //ProcessRequest方法的构建(ProcessRequest Construction of the method)
            private static String processRequest(String line, PrintWriter out, BufferedReader in) {
                //将命令行按空格分割为最多三个部分(Split the command line into a maximum of three parts by space)
                String[] parts = line.split(" ", 3);
                if (parts.length < 2) {
                    return line + ": ERR bad request";
                }
            
                //获取命令的操作类型并转换为大写(Get the type of command and convert it to uppercase)
                String operation = parts[0].toUpperCase();
                //获取命令的键(Get the key for the command)
                String key = parts[1];
                //获取命令的值（如果有的话）  (Get the value of the command (if any))
                String value = parts.length > 2 ? parts[2] : "";
            
                //验证是否是三种操作中的一种(Verify that it is one of the three operations)
                if (!operation.equals("PUT") && !operation.equals("READ") && !operation.equals("GET")) {
                    return line + ": ERR bad operation";
                }
                if (!isPrintable(key) || (!value.isEmpty() && !isPrintable(value))) {
                    return line + ": ERR non-printable characters";
                }
            
                String message;///局部变量，用来储存创建的服务器信息(A local variable that stores the created server information)
                //三种操作的对应格式
                if (operation.equals("PUT")) {
                    //检查键或值的长度是否超过最大限制
                    if (key.length() > MAX_KEY_VALUE_LENGTH || value.length() >MAX_KEY_VALUE_LENGTH){
                        return line + ": ERR key or value too long";
                    }
                    //检查元组大小（键+值+分隔符）是否超过最大限制
                    if (key.length() + value.length() + 1 > MAX_KEY_VALUE_LENGTH){
                        return line + ":ERR tuple too large";

                    }
               
                message = "P " + key + " " + value;
                } else if (operation.equals("READ")) {
                    message = "R " + key;
                } else {
                    message = "G " + key;
                }
            
                int length = message.length() + 4;//计算信息的总长度(Calculate the total length of information)
                String fullMessage = String.format("%03d %s", length, message);
            
                //捕获异常
                try {
                    out.println(fullMessage);
                    String response = in.readLine();
                    //检测连接是否关闭(Check if the connection is closed)
                    if (response == null) {
                        return line + ": ERR server gone";
                    }
                    return line + ": " + response.substring(4);
                } catch(SocketTimeoutException e){
                    return line + ": ERR server timeout";
                }catch (Exception e) {
                    return line + ": ERR can't talk to server";
                }
            }

            //定义一个名为isprintable的私有类
            private static boolean isPrintable(String str) {
                for (char c : str.toCharArray()) {
                    if (c < 32 || c > 126) {
                        return false;
                    }
                }
                return true;
            }
    }
