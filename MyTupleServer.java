import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

//定义了一个名为 TupleSpaceServer的公共类，其为程序的主类。(A public class named TupleSpaceServer is defined as the main class of the program)
public class MyTupleServer {
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
    private static final int MAX_KEY_VALUE_LENGTH = 999;
    private static final int MAX_TUPLE_SIZE = 970;
    private static final int STATS_INTERVAL = 10000;
    
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Please give a port number!");
            return;
        }

        int port; 
        try {
            port = Integer.parseInt(args[0]);
            if (port < MIN_PORT || port > MAX_PORT) {
                System.out.println("Port must be between 50000 and 59999!");
                return;
            }
        } catch (Exception e) {
            System.out.println("Invalid port number!");
            return;
        }

        //I will create and initialize a new thread object for periodically printing statistics)
        Thread statsThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    printStats();
                    try {
                        Thread.sleep(STATS_INTERVAL);
                    } catch (Exception e) {
                        System.out.println("Stats thread error!");
                    }
                }
            }
        });
        statsThread.start();

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client: " + clientSocket.getInetAddress());
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
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

    //创建printStats方法(Create the printStats method)
    private static void printStats(){
        synchronized (tupleServer) {
            int numTuples = tupleServer.size();
            double avgTupleSize = 0.0;
            double avgKeySize = 0.0;
            double avgValueSize = 0.0;
    
            if (numTuples > 0) {
                int totalKeySize = 0;
                int totalValueSize = 0;
                for (String key : tupleServer.keySet()) {
                    totalKeySize += key.length();
                    totalValueSize += tupleServer.get(key).length();
                }
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

    //再定义一个静态内部类ClientHandler来实现Runnable接口(Define a static inner class ClientHandler to implement the Runnable interface)
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;  
        private BufferedReader in;
        private PrintWriter out;

        
        //构造方法并初始化ClientHandler
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            //原本是想使用synchronized以statsLock为锁，再不断累加totalClients计数器来确保线程安全的
            //(Originally, I wanted to use synchronized with statsLock as the lock and continuously accumulate the totalClients counter to ensure thread safety)
            //但是不知道为什么Statslock和totalClients++都报错了(But I don't know why both Statslock and totalClients++ are wrong)
            synchronized (statsLock) {
                totalClients++;
            }
        }

        //实现run方法
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String request;
                while ((request = in.readLine()) != null) {
                    synchronized (statsLock) {
                        totalOperations++;
                    }
                    String response = handleRequest(request);
                    out.println(response);
                }
            } catch (Exception e) {
                System.out.println("Client error: " + e.getMessage());
                synchronized (statsLock) {
                    errorCount++;
                }
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (clientSocket != null) clientSocket.close();
                } catch (Exception e) {
                    System.out.println("Error closing client!");
                }
            }
        }




        
    
        //将handleRequest方法定义为负责解析客户端请求并执行相应的元组操作(The handleRequest method is defined to parse client requests and perform the corresponding tuple operations)
        private String handleRequest(String request) {
            //按照要求验证请求长度是否至少为7个字符,还有前三个字符是否为数字（会提取前三个字符）
            //Verify that the request length is at least 7 characters as required, and that the first three characters are numeric (the first three characters will be extracted)
            if (request.length() < 7 || !isNumber(request.substring(0,3))) {
                synchronized (statsLock) {
                    errorCount++;
                }
                return makeResponse("ERR invalid request");
            }
    
            //对请求的前3个字符作为请求大小并尝试解析(Take the first three characters of the request as the size of the request and try to parse it)
            int size;
            try {
                size = Integer.parseInt(request.substring(0, 3));
            } catch (Exception e) {
                synchronized (statsLock) {
                    errorCount++;
                }
                return makeResponse("ERR invalid size");
            }
    
            //检查请求的实际长度是否与声明的大小相匹配，不匹配则返回错误相应(Check whether the actual length of the request matches the declared size, and return an error response if they do not match)
            if (request.length() != size || request.charAt(3) != ' ' || request.charAt(5) != ' ') {
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
    

            //调用isPrintable方法和isEmpty()方法检查key和value是否包含非可打印字符(Call the isPrintable method and the isEmpty() method to check whether the key and value contain non-printable characters)
            if (!isPrintable(key) ||(!value.isEmpty()&&!isPrintable(value))) {
                synchronized(statsLock){
                    errorCount++;
                }
                return makeResponse("ERR non-printable characters");
                
            }
            synchronized (tupleServer) {
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
                    synchronized (statsLock) {
                        putCount++;//记录次数
                    }
                    if(key.length() > MAX_KEY_VALUE_LENGTH || value.length() > MAX_KEY_VALUE_LENGTH){
                        synchronized(statsLock){
                            errorCount++;//记录错误发生次数
                        }
                        return makeResponse("ERR key or value too long");
                    }
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
    

        //定义一个名为isPrintable 的私有方法(Define a private method called isPrintable)
        private boolean isPrintable(String str) {
            //使用增强型for循环（foreach）遍历字符串str的每个字符(Use the enhanced for loop (foreach) to traverse each character of the string str)
            for (char c : str.toCharArray()) {
                if (c < 32 || c > 126) {
        
                    return false;
                }
            }
            return true;
        }

        //定义一个私有方法isNumber用来检查输入字符串是否只包含数字字符(Define a private method isNumber to check whether the input string contains only numeric characters)
        private boolean isNumber(String str) {
            //遍历每一个字符并逐个检查(Go through each character and check it one by one)
            for (char c : str.toCharArray()) {
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            return true;
        }
    
        //格式化响应字符串
        private String makeResponse(String message) {
            int length = message.length();
            //从而添加3位长度前缀(Thus, a 3-bit prefix is added)
            return String.format("%03d %s", length, message);
        }
    }
}