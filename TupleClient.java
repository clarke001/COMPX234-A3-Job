import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

//定义一个名为TupleClient的公共类。可使用socket连接到服务器。（Define a public class called TupleClient. You can use a socket to connect to the server.）
public class TupleClient {
    private static int TIMEOUT = 10000;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Need hostname, port, and file name!");
            return;
        }

        String hostname = args[0];
        int port;  //储存端口号
        try {
            port = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Port number is wrong!");
            return;
        }

        String filename = args[2];
        Socket socket = null;//与服务器建立联系(Connect to the server)
        PrintWriter out = null;//向服务器发送数据(Send data to the server)
        BufferedReader in =null;//读取服务器响应(Read the server response)
        BufferedReader filReader = null;//读取本地文件(Read local files)

        try {
            socket = new Socket(hostname, port);
            socket.setSoTimeout(TIMEOUT); 
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            filReader = new BufferedReader(new FileReader(filename));

            String line;
            while ((line = filReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
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
                String[] parts = line.split(" ", 3);
                if (parts.length < 2) {
                    return line + ": ERR bad request";
                }
                String operation = parts[0].toUpperCase();
                String key = parts[1];
                String value = parts.length > 2 ? parts[2] : "";
        
                if (!operation.equals("PUT") && !operation.equals("READ") && !operation.equals("GET")) {
                    return line + ": ERR bad operation";
                }
                if (!isPrintable(key) || (!value.isEmpty() && !isPrintable(value))) {
                    return line + ": ERR non-printable characters";
                }
            
                String message;///局部变量，用来储存创建的服务器信息(A local variable that stores the created server information)
                //三种操作的对应格式
                if (operation.equals("PUT")) {
                    if (key.length() > MAX_KEY_VALUE_LENGTH || value.length() >MAX_KEY_VALUE_LENGTH){
                        return line + ": ERR key or value too long";
                    }
                    if (key.length() + value.length() + 1 > MAX_KEY_VALUE_LENGTH){
                        return line + ":ERR tuple too large";

                    }
               
                message = "P " + key + " " + value;
                } else if (operation.equals("READ")) {
                    message = "R " + key;
                } else {
                    message = "G " + key;
                }
            
                int length = message.length() + 4;
                String fullMessage = String.format("%03d %s", length, message);
            
                //捕获异常
                try {
                    out.println(fullMessage);
                    String response = in.readLine();
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
