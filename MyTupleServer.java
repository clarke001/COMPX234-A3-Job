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
    //定义键和值的最大长度
    private static final int MAX_KEY_VALUE_LENGTH = 999;
    //定义元组（键+值）的最大可行大小
    private static final int MAX_TUPLE_SIZE = 970;


    //十秒为一次间隔打印数据
    private static final int STATS_INTERVAL = 10000;
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

        //创建并初始化一个新的线程对象用于定时打印统计信息
        Thread statsThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    printStats();//调用printStats()方法
                    try {
                        Thread.sleep(STATS_INTERVAL);
                    } catch (Exception e) {
                        System.out.println("Stats thread error!");
                    }
                }
            }
        });
        statsThread.start();//启动统计线程

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

    //创建printStats方法
    private static void printStats(){
        //上锁确保安全
        synchronized (tupleServer) {
            //获取元组空间中的元组数量
            int numTuples = tupleServer.size();
            double avgTupleSize = 0.0;
            double avgKeySize = 0.0;
            double avgValueSize = 0.0;
    
            //检查是否为空
            if (numTuples > 0) {
                int totalKeySize = 0;
                int totalValueSize = 0;
                //遍历元组空间中的所有键
                for (String key : tupleServer.keySet()) {
                    totalKeySize += key.length();
                    totalValueSize += tupleServer.get(key).length();//累加当前键对应值的字符长度
                }
                //计算平均值
                avgKeySize = (double) totalKeySize / numTuples;
                avgValueSize = (double) totalValueSize / numTuples;
                avgTupleSize = avgKeySize + avgValueSize;
            }
    
            //Print relevant calculation information
            synchronized (statsLock) {
                System.out.println("\n--- Tuple Space Stats ---");
                System.out.println("Number of Tuples: " + numTuples);
                System.out.println("Average Tuple Size: " + avgTupleSize);
                System.out.println("Average Key Size: " + avgKeySize);
                System.out.println("Average Value Size: " + avgValueSize);
                System.out.println("Total Clients: " + totalClients);
                System.out.println("Total Operations: " + totalOperations);
                System.out.println("READs: " + readCount);
                System.out.println("GETs: " + getCount);
                System.out.println("PUTs: " + putCount);
                System.out.println("Errors: " + errorCount);
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

        //实现run方法
        public void run() {
            try {
                //为了获取socket的字节输出与输入，然后再继续读取与写入
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String request;
                //持续循环读取客户端方式的请求
                //逐行读取
                while ((request = in.readLine()) != null) {
                    //使用synchronized (statsLock)确保安全
                    synchronized (statsLock) {
                        totalOperations++;
                    }
                    //调用handleRequest方法处理请求并将响应发送给客户端
                    String response = handleRequest(request);
                    out.println(response);
                }
            } catch (Exception e) {
                System.out.println("Client error: " + e.getMessage());
                synchronized (statsLock) {
                    errorCount++;
                }
            } finally {
                //仍然是为了关闭资源
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (clientSocket != null) clientSocket.close();
                } catch (Exception e) {
                    System.out.println("Error closing client!");
                }
            }
        }




        
    
        //将handleRequest方法定义为负责解析客户端请求并执行相应的元组操作
        private String handleRequest(String request) {
            //按照要求验证请求长度是否至少为7个字符
            if (request.length() < 7) {
                synchronized (statsLock) {
                    errorCount++;
                }
                return makeResponse("ERR invalid request");
            }
    
            //对请求的前3个字符作为请求大小
            int size;
            try {
                size = Integer.parseInt(request.substring(0, 3));
            } catch (Exception e) {
                synchronized (statsLock) {
                    errorCount++;
                }
                return makeResponse("ERR invalid size");
            }
    
            //检查请求的实际长度是否与声明的大小相匹配，不匹配则返回错误相应
            if (request.length() != size) {
                synchronized (statsLock) {
                    errorCount++;
                }
                return makeResponse("ERR size mismatch");
            }
    
            char command = request.charAt(4);
            //解析命令后的内容再提取key和可选的value
            String[] parts = request.substring(6).split(" ", 2);
            String key = parts[0];
            String value = parts.length > 1 ? parts[1] : "";
    
            //同步防止多个客户端线程同时修改tupleSpace
            synchronized (tupleServer) {
                //处理READ命令（R）并读取指定key的值
                if (command == 'R') {
                    synchronized (statsLock) {
                        readCount++;
                    }
                    if (tupleServer.containsKey(key)) {
                        String val = tupleServer.get(key);
                        return makeResponse("OK (" + key + ", " + val + ") read");
                    } else {
                        synchronized (statsLock) {
                            errorCount++;
                        }
                        return makeResponse("ERR " + key + " does not exist");
                    }
                } else if (command == 'G') {//处理GET命令（G）再获取并删除指定key的键值对
                    synchronized (statsLock) {
                        getCount++;
                    }
                    if (tupleServer.containsKey(key)) {
                        String val = tupleServer.remove(key);
                        return makeResponse("OK (" + key + ", " + val + ") removed");
                    } else {
                        synchronized (statsLock) {
                            errorCount++;
                        }
                        return makeResponse("ERR " + key + " does not exist");
                    }
                } else if (command == 'P') {//处理命令（P），添加新的键值对（操作分支）
                    //上锁确保安全
                    synchronized (statsLock) {
                        putCount++;//记录次数
                    }
                    //检查键或值的长度有没有超过最大限制
                    if(key.length() > MAX_KEY_VALUE_LENGTH || value.length() > MAX_KEY_VALUE_LENGTH){
                        synchronized(statsLock){
                            errorCount++;//记录错误发生次数
                        }
                        return makeResponse("ERR key or value too long");
                    }
                    //检查元组大小（键+值+分隔符）是否超过最大限制
                    if (key.length() + value.length() + 1 > MAX_TUPLE_SIZE) {
                        synchronized (statsLock) {
                            errorCount++;//再次自增 errorCount用来记录元组太大的错误
                        }
                        return makeResponse("ERR tuple too large");
                    }
                    if (tupleServer.containsKey(key)) {//无效命令处理，检测键是否已经存在于元组空间
                        synchronized (statsLock) {
                            errorCount++;
                        }
                        return makeResponse("ERR " + key + " already exists");
                    } else {
                        tupleServer.put(key, value);
                        return makeResponse("OK (" + key + ", " + value + ") added");
                    }
                } else {
                    synchronized (statsLock) {
                        errorCount++;
                    }
                    return makeResponse("ERR invalid command");
                }
            }
        }
    
        //格式化响应字符串
        private String makeResponse(String message) {
            int length = message.length();
            //从而添加3位长度前缀
            return String.format("%03d %s", length, message);
        }
    }
}