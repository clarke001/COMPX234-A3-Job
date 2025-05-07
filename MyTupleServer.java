import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

//定义了一个名为 TupleSpaceServer的公共类，其为程序的主类。
public class MyTupleServer {
    //对服务器允许的最小和最大端口号常量进行设定。
    //定义多个变量计数器，来限制并跟踪服务器的运行情况（当然部分的变量目前我还没有使用并确定其具体的属性）
    private static Map<String , String> tupleServer = new HashMap<>();
    private static int totalClients = 0;
    private static int totalOperations = 0;
    private static int readCount = 0;
    private static int getCount = 0;
    private static int putCount = 0;
    private static int errorCount = 0;
    private static Object statsLock = new Object();
    private static final int MIN_PORT = 50000;
    private static final int MAX_PORT = 59999;

    //程序的主方法
    public static void main(String[] args) {

        //检查命令行是否提供了一个合适的参数（端口号），若不是（即参数不是1）则打印错误信息。
        if (args.length != 1) {
            System.out.println("Please give a port number!");
            return;
        }

        int port;  //试将第一个命令行参数转换为整数以作为端口号
        try {
            //使用try...catch是因为parseInt会抛出错误。
            port = Integer.parseInt(args[0]);
            //验证端口的范围是否合规
            if (port < MIN_PORT || port > MAX_PORT) {
                System.out.println("Port must be between 50000 and 59999!");
                return;
            }
        } catch (Exception e) {
            System.out.println("Invalid port number!");
            return;
        }
        //声明一个初始值为NULL的ServerSocket变量
        ServerSocket serverSocket = null;
        //尝试在指定的窗口创建并连接serverSocket
        //serverSocket可能会抛出异常所以使用try...catch进行捕获
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            //添加了while会使进程进入无限循环并等待客户端连接。
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client: " + clientSocket.getInetAddress());
                //为每个客户端创建并启动一个新线程（允许服务器并发处理多个独立的客户端）
                //创建了ClientHandler对象和clientThead对象
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();//调用了start（）方法启动线程
            }
        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    System.out.println("Error closing server!");
                }
            }
        }
    }


    //再定义一个静态内部类ClientHandler来实现Runnable接口
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;  //变量用于存储与客户端的连接
        private BufferedReader in;
        private PrintWriter out;

        
        //构造方法并初始化ClientHandler
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            //原本是想使用synchronized以statsLock为锁，再不断累加totalClients计数器来确保线程安全的
            //但是不知道为什么Statslock和totalClients++都报错了
            synchronized (statsLock) {
                totalClients++;
            }
        }

        //实现run方法，初步实现的很简单只有试试可否关闭就行。
        public void run() {
            try {
                //初始化in字段以准备读取客户端发送的文本数据（按行读）
                //初始化out字段以准备向客户端发送文本响应
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String request;
                //持续读取客户端的请求
            while ((request = in.readLine()) != null) {
                //使用synchronized锁定statsLock，确保线程安全
                synchronized (statsLock) {
                    totalOperations++;
                }
                // 临时响应，稍后实现 handleRequest
                out.println("OK request received: " + request);
                //clientSocket.close();
                }
            } catch (Exception e) {
                System.out.println("Client error: " + e.getMessage());
                synchronized (statsLock) {
                    errorCount++;
                }
            } finally {
                //close
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (clientSocket != null) clientSocket.close();
                }catch (Exception e) {
                System.out.println("Error closing client!");
            }
        }
    }
}
}