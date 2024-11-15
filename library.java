import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class library {

    private static final String CONFIG_FILE = "config.txt";
    private static final String Book_URL = "https://mipservice.tit.edu.cn/consumeServer/ReadingRoomWx/userSchLabApply";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 获取当前时间

    public static void main(String[] args) {
        LocalTime now = LocalTime.now();
        // 设置开始时间
        LocalTime Begin = LocalTime.of(20, 29);
        if (isConfigIncomplete()) {
            CreateConfig();//加载或创建配置文件
        }
        else if(ConfigComplete()){
            JOptionPane.showMessageDialog(null, "配置文件内容不完整，请补充！", "配置缺失", JOptionPane.ERROR_MESSAGE);
            CreateConfig();//加载或创建配置文件
        }
        else if((now.equals(Begin) || now.isAfter(Begin))&&TestSystemDelay()) {

            Runnable task = createTask();
            task.run();
            System.exit(0);
        }else{
            JOptionPane.showMessageDialog(null,"该时段不是规定的预约时段！","预约出错", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Runnable createTask() {
        LocalDate today = LocalDate.now();

        String cardNumber = getConfig("学号");
        String labRoomId = getConfig("自习室");
        String seatNum = getConfig("座位号");
        String labRoomId1 = getConfig("备选自习室");
        String seatNum1 = getConfig("备选座位号");
        String roomName = getRoomName(labRoomId);
        String roomName1 = getRoomName(labRoomId1);


        return () -> {

            LocalDate tomorrowafter = today.plusDays(2);
            String tomorrowafterDateStr = tomorrowafter.format(formatter);
            String jsonData = loadJsonData(tomorrowafterDateStr);
            String jsonData1 = loadJsonData1(tomorrowafterDateStr);

            //第一次预约，首选预约
            sendRequest(jsonData);

            //当首选座位被约，立即预约备选座位预约
            if (responseBody.contains("座位已经 在当前时间段已经被预约")) {
                JOptionPane optionPane = new JOptionPane(cardNumber + "  预约失败！ "+"【首选座位】: " + roomName + "的" + seatNum + "号座位 已经被约！ 尝试预约【备选座位】······", JOptionPane.INFORMATION_MESSAGE);
                JDialog dialog = optionPane.createDialog(null, "预约失败");
                dialog.setModal(false); // 设置为非模态
                dialog.setVisible(true);

                try {
                    TimeUnit.SECONDS.sleep(5);
                    dialog.dispose(); // 关闭第一次预约失败的对话框
                    TimeUnit.SECONDS.sleep(5);
                    sendRequest(jsonData1);//第二次预约，备选预约

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if(responseBody.contains("座位已经 在当前时间段已经被预约")){
                    JOptionPane.showMessageDialog(null,cardNumber + "  预约失败！ "+"【备选座位】: "+roomName1+"的"+seatNum1+"号座位 已经被约","预约失败", JOptionPane.ERROR_MESSAGE);
                }
                else if (responseBody.contains("4. 接受预约 校验通过后 进行预约成功")){
                    JOptionPane.showMessageDialog(null, cardNumber+"  预约成功！ "+" 预约【被选座位】: "+roomName1+"的"+seatNum1+"号座位成功");
                }

            }

            // 检查并显示消息
            else if (responseBody.contains("4. 接受预约 校验通过后 进行预约成功")){
                JOptionPane.showMessageDialog(null, cardNumber+"  预约成功！ "+" 预约【首选座位】: "+roomName+"的"+seatNum+"号座位成功");
            }
            else if (responseBody.contains("当前用户在 所选择时间周期内 已预约")) {
                JOptionPane.showMessageDialog(null, cardNumber+"  该时间段已有预约,请勿重复预约","重复预约", JOptionPane.ERROR_MESSAGE);
            }
            else if (responseBody.contains("4. 接受预约 校验通过后 进行预约参数缺失 或者日期非今日明日后日")) {
                JOptionPane.showMessageDialog(null, "  预约参数缺失 或 不在规定预约时间内","预约出错", JOptionPane.ERROR_MESSAGE);
            }


        };
    }


    //测试预约系统的开放时间
    private static boolean TestSystemDelay(){
            String urlString = "https://mipservice.tit.edu.cn/consumeServer/ReadingRoomWx/getReadyForOneSchLabApply";
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 设置请求方法和请求头
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("Origin", "https://mipweb.tit.edu.cn");
                connection.setRequestProperty("Sec-Fetch-Site", "same-site");
                connection.setRequestProperty("Sec-Fetch-Mode", "cors");
                connection.setRequestProperty("Sec-Fetch-Dest", "empty");
                connection.setRequestProperty("Referer", "https://mipweb.tit.edu.cn");
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
                connection.setDoOutput(true);

                LocalDate today = LocalDate.now();
                String cardNumber = getConfig("学号");
                String labRoomId = getConfig("自习室");
                String seatNum = getConfig("座位号");

                LocalDate tomorrowafter = today.plusDays(2);
                String tomorrowafterDateStr = tomorrowafter.format(formatter);
                String jsonInputString=String.format("{\"cardNumber\": \"%s\", \"applyDate\": \"%s\", \"applyStartTime\": \"06:30\", \"applyDuration\": \"15.5\", \"labRoomId\": \"%s\", \"seatNum\": \"%s\"}",
                        cardNumber, tomorrowafterDateStr, labRoomId, seatNum);

                // 构建请求体
                try(OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // 读取响应
                int responseCode = connection.getResponseCode();

                System.out.println("Response Code: " + responseCode);
                boolean continueLoop = true;

                try(Scanner scanner = new Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A")) {
                    if (scanner.hasNext()) {
                        String response = scanner.next();
                        System.out.println("Response: " + response);

                 while(continueLoop) {
                    if (response.contains("当前用户在 所选择时间周期内 已预约")) {
                        continueLoop = false;
                    } else if (!response.contains(" 3. 预约准备 查询某一个阅览室的 日期 时间的 座位空闲情况成功")) {

                        //System.out.println("等待时间: " + delay + "ms");//测试等待时间
                        //TimeUnit.MILLISECONDS.sleep(50);
                        TestSystemDelay();

                    }

                    else if(response.contains(" 3. 预约准备 查询某一个阅览室的 日期 时间的 座位空闲情况成功")){
                        continueLoop = false;
                    }
                    }
                 return true;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        return false;
    }




    private static String responseBody;
    // 发送HTTP请求-预约座位的方法
    private static void sendRequest(String jsonData) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Book_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .header("Origin", "https://mipweb.tit.edu.cn")
                .header("Referer", "https://mipweb.tit.edu.cn/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36 Edg/112.0.1722.39")
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println("Status Code: " + response.statusCode());
            responseBody = response.body();
            System.out.println("Response Body: " + responseBody);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    //创建楼层-自习室的集合
    private static String[] getRoomNames() {
        return new String[]{
                "二层-报刊阅览室",
                "三层-中文阅览室一",
                "三层-中文阅览室二",
                "三层-休闲区",
                "四层-中文阅览室三",
                "四层-中文阅览室四",
                "五层-外文阅览室",
                "五层-多功能阅览室",
                "五层-综合阅览室",
                "六层-多媒体阅览室",
                "六层-电子阅览室"};
    }

    //创建楼层-自习室指向数字的hashmap
    private static String getRoomId(String roomName) {
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("二层-报刊阅览室", "1");
        roomMap.put("三层-中文阅览室一", "2");
        roomMap.put("三层-中文阅览室二", "3");
        roomMap.put("三层-休闲区", "4");
        roomMap.put("四层-中文阅览室三", "5");
        roomMap.put("四层-中文阅览室四", "6");
        roomMap.put("五层-外文阅览室", "7");
        roomMap.put("五层-多功能阅览室", "16");
        roomMap.put("五层-综合阅览室", "8");
        roomMap.put("六层-多媒体阅览室", "9");
        roomMap.put("六层-电子阅览室", "10");
        return roomMap.getOrDefault(roomName, "1"); // 默认为 二层-报刊阅览室
    }

    //创建楼层-自习室指向数字的hashmap
    private static String getRoomId1( String roomName) {
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("二层-报刊阅览室", "1");
        roomMap.put("三层-中文阅览室一", "2");
        roomMap.put("三层-中文阅览室二", "3");
        roomMap.put("三层-休闲区", "4");
        roomMap.put("四层-中文阅览室三", "5");
        roomMap.put("四层-中文阅览室四", "6");
        roomMap.put("五层-外文阅览室", "7");
        roomMap.put("五层-多功能阅览室", "16");
        roomMap.put("五层-综合阅览室", "8");
        roomMap.put("六层-多媒体阅览室", "9");
        roomMap.put("六层-电子阅览室", "10");
        return roomMap.getOrDefault(roomName, "1"); // 默认为 二层-报刊阅览室
    }

    private static String getRoomName(String roomId) {
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("1", "二层-报刊阅览室");
        roomMap.put("2", "三层-中文阅览室一");
        roomMap.put("3", "三层-中文阅览室二");
        roomMap.put("4", "三层-休闲区");
        roomMap.put("5", "四层-中文阅览室三");
        roomMap.put("6", "四层-中文阅览室四");
        roomMap.put("7", "五层-外文阅览室");
        roomMap.put("16", "五层-多功能阅览室");
        roomMap.put("8", "五层-综合阅览室");
        roomMap.put("9", "六层-多媒体阅览室");
        roomMap.put("10", "六层-电子阅览室");
        return roomMap.getOrDefault(roomId, "二层-报刊阅览室"); // 默认为 二层-报刊阅览室
    }

    //将每层的自习室对应的座位数量做出限制
    private static final Map<String, Integer[]> roomSeatRanges = new HashMap<>();
    static {
        roomSeatRanges.put("二层-报刊阅览室", new Integer[]{1, 307});
        roomSeatRanges.put("三层-中文阅览室一", new Integer[]{1, 307});
        roomSeatRanges.put("三层-中文阅览室二", new Integer[]{1, 307});
        roomSeatRanges.put("三层-休闲区", new Integer[]{1, 56});
        roomSeatRanges.put("四层-中文阅览室三", new Integer[]{1, 288});
        roomSeatRanges.put("四层-中文阅览室四", new Integer[]{1, 288});
        roomSeatRanges.put("五层-外文阅览室", new Integer[]{1,416});
        roomSeatRanges.put("五层-多功能阅览室", new Integer[]{1,216});
        roomSeatRanges.put("五层-综合阅览室", new Integer[]{1, 416});
        roomSeatRanges.put("六层-多媒体阅览室", new Integer[]{1, 212});
        roomSeatRanges.put("六层-电子阅览室", new Integer[]{1, 288});
    }
    private static final Map<String, Integer[]> roomSeatRange = new HashMap<>();
    static {
        roomSeatRange.put("二层-报刊阅览室", new Integer[]{1, 307});
        roomSeatRange.put("三层-中文阅览室一", new Integer[]{1, 307});
        roomSeatRange.put("三层-中文阅览室二", new Integer[]{1, 307});
        roomSeatRange.put("三层-休闲区", new Integer[]{1, 56});
        roomSeatRange.put("四层-中文阅览室三", new Integer[]{1, 288});
        roomSeatRange.put("四层-中文阅览室四", new Integer[]{1, 288});
        roomSeatRange.put("五层-外文阅览室", new Integer[]{1,416});
        roomSeatRange.put("五层-多功能阅览室", new Integer[]{1,216});
        roomSeatRange.put("五层-综合阅览室", new Integer[]{1, 416});
        roomSeatRange.put("六层-多媒体阅览室", new Integer[]{1, 212});
        roomSeatRange.put("六层-电子阅览室", new Integer[]{1, 288});
    }
    //创建配置文件的UI面板
    private static void CreateConfig() {
        JFrame frame = new JFrame("座位预约");//设置窗口的title
        frame.setResizable(false); //设置窗口不可调整大小
        frame.setSize(250, 300); // 设置窗口的初始大小
        ImageIcon icon = new ImageIcon("resource//IMG\\座位预约.png");
        frame.setIconImage(icon.getImage());//给窗体设置图标方法

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//设置窗口关闭后关闭程序
        frame.setLocationRelativeTo(null);//居中定位

        JPanel panel = new JPanel();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel labelCardNumber = new JLabel("学  号:");
        labelCardNumber.setFont(new Font("宋体", Font.BOLD, 13));
        JTextField textFieldCardNumber = new JTextField(13);
        textFieldCardNumber.setPreferredSize(new Dimension(14, 25));
        textFieldCardNumber.setFont(new Font("宋体", Font.BOLD, 18));

        JLabel labelRoomName = new JLabel("自习室:");
        JComboBox<String> comboBoxRoomName = new JComboBox<>(getRoomNames());
        labelRoomName.setFont(new Font("宋体", Font.BOLD, 13));
        comboBoxRoomName.setBackground(Color.WHITE); // 设置背景色为白色
        comboBoxRoomName.setForeground(Color.BLACK); // 设置前景色（文本颜色）为黑色
        comboBoxRoomName.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255))); // 设置下拉框的边框色为黑色

        JLabel labelSeatNum = new JLabel("座位号:");
        labelSeatNum.setFont(new Font("宋体", Font.BOLD, 13));
        JTextField textFieldSeatNum = new JTextField(13);
        textFieldSeatNum.setPreferredSize(new Dimension(14, 25));
        textFieldSeatNum.setFont(new Font("宋体", Font.BOLD, 18));



        JLabel labelRoomName1 = new JLabel("备选室:");
        JComboBox<String> comboBoxRoomName1 = new JComboBox<>(getRoomNames());
        labelRoomName1.setFont(new Font("宋体", Font.BOLD, 13));
        comboBoxRoomName1.setBackground(Color.WHITE); // 设置背景色为白色
        comboBoxRoomName1.setForeground(Color.BLACK); // 设置前景色（文本颜色）为黑色
        comboBoxRoomName1.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255))); // 设置下拉框的边框色为黑色

        JLabel labelSeatNum1 = new JLabel("备选号:");
        labelSeatNum1.setFont(new Font("宋体", Font.BOLD, 13));
        JTextField textFieldSeatNum1 = new JTextField(13);
        textFieldSeatNum1.setPreferredSize(new Dimension(14, 25));
        textFieldSeatNum1.setFont(new Font("宋体", Font.BOLD, 18));

        JButton buttonSave = new JButton("保   存");
        buttonSave.setFont(new Font("微软雅黑", Font.BOLD, 13));
        buttonSave.setBackground(new Color(0, 122, 255));
        buttonSave.setForeground(Color.WHITE);


        JButton buttonBook = new JButton("预约");
        buttonBook.setFont(new Font("微软雅黑", Font.BOLD, 13));
        buttonBook.setBackground(new Color(255, 255, 255, 255));
        buttonBook.setForeground(Color.GRAY);



        buttonSave.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String cardNumber = textFieldCardNumber.getText().trim();
                        String roomName = (String) comboBoxRoomName.getSelectedItem();
                        String roomName1 = (String) comboBoxRoomName1.getSelectedItem();

                        String labRoomId = getRoomId(roomName);
                        String labRoomId1 = getRoomId1(roomName1);

                        String seatNumStr = textFieldSeatNum.getText().trim();
                        String seatNumStr1 = textFieldSeatNum1.getText().trim();

                        if (cardNumber.isEmpty() || seatNumStr.isEmpty()|| seatNumStr1.isEmpty()) {
                            JOptionPane.showMessageDialog(frame, "学号、座位号、备选座位号不能为空！");
                            return;
                        }
                        if(!isPositiveInteger(cardNumber)||!isPositiveInteger(seatNumStr)||!isPositiveInteger(seatNumStr1)){
                            JOptionPane.showMessageDialog(frame, "学号、座位号、备选座位号必须是数字！");
                            return;
                        }
                        if(seatNumStr.equals(seatNumStr1) && labRoomId.equals(labRoomId1)) {
                            JOptionPane.showMessageDialog(frame, "座位号和备选座位号不能相同！");
                            return;
                        }

                        try {
                            int seatNum = Integer.parseInt(seatNumStr);
                            int seatNum1 = Integer.parseInt(seatNumStr1);

                            Integer[] range = roomSeatRanges.get(roomName);
                            Integer[] range1 = roomSeatRange.get(roomName1);

                            if(seatNum < range[0] || seatNum > range[1]||seatNum1 < range1[0] || seatNum1 > range1[1]){
                                if(roomName.equals(roomName1)){
                                    JOptionPane.showMessageDialog(frame, "座位号或备选座位号填写错误！"+roomName+"的座位号范围：" + range[0] + "-" + range[1],"信息错误", JOptionPane.ERROR_MESSAGE);
                                }else{
                                    JOptionPane.showMessageDialog(frame, "座位号或备选座位号填写错误！"+"\n"+roomName+"的座位号范围：" + range[0] + "-" + range[1]+"\n"+roomName1+"的座位号范围：" + range1[0] + "-" + range1[1],"信息错误", JOptionPane.ERROR_MESSAGE);
                                }
                                return;

                            }

                            // 保存配置
                            saveConfig(cardNumber, labRoomId, seatNum,labRoomId1,seatNum1);
                            JOptionPane.showMessageDialog(null, "保存配置成功！");
                            frame.dispose();

                        } catch (NumberFormatException ex) {
                        }

                    }
                });

        panel.add(labelCardNumber);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(textFieldCardNumber);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelRoomName);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(comboBoxRoomName);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelSeatNum);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(textFieldSeatNum);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));


        panel.add(labelRoomName1);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(comboBoxRoomName1);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(labelSeatNum1);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(textFieldSeatNum1);

        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(35));

        panel.add(buttonSave);
        panel.add(Box.createHorizontalStrut(0));
        panel.add(Box.createVerticalStrut(50));

        frame.add(panel);
        frame.setVisible(true);
    }

    //使用正则表达式来匹配字符串为正整数，不含有空格等其他字符
    public static boolean isPositiveInteger(String str) {
        Pattern pattern = Pattern.compile("^[0-9]\\d*$");
        Matcher matcher = pattern.matcher(str.trim()); // 使用trim()去除前后空格
        return matcher.matches();
    }


    // 判断文件不存在或读取错误时认为配置不完整
    private static boolean isConfigIncomplete() {
        File configFile = new File(CONFIG_FILE_PATH);
        if(configFile.exists()) {
            try {

                // 读取文件内容
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE_PATH)));
                // 检查文件内容是否为空
                return content.trim().isEmpty();

            } catch (IOException e) {
                return true;
            }
        }else {
            return true;
        }
    }



    private static boolean ConfigComplete() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("=")){
                    String[] parts = line.split("=", 2); // 限制分割为两部分
                    if (parts.length > 1) {
                        String beforeEqual = parts[0].trim(); // 去除前后空白字符
                        String afterEqual = parts[1].trim(); // 去除前后空白字符

                        // 检查等号前后的内容是否都存在
                        if (beforeEqual.isEmpty() || afterEqual.isEmpty())
                        {
                        return true; // 如果等号前后任一部分为空，返回
                        }
                    }else{
                        return true; // 如果没有等号后面的部分，返回
                    }
                }else{
                    return true;
                }
            }

        } catch (IOException e) {
            return true; // 发生异常时，返回
        }
        return false; // 所有行都检查完毕，没有发现等号前后没有内容的情况
    }

    //保存创建的config.txt配置文件，获取填写在面板的信息并写入文件
    private static void saveConfig(String cardNumber, String labRoomId, int seatNum,String labRoomId1,int seatNum1) {
        String content = String.format("学号=%s\n自习室=%s\n座位号=%s\n备选自习室=%s\n备选座位号=%s\n", cardNumber, labRoomId, seatNum,labRoomId1,seatNum1);
        try {
            Files.writeString(Paths.get(CONFIG_FILE), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载配置文件中的信息
    private static String loadJsonData(String dateStr) {
        String cardNumber = getConfig("学号");
        String labRoomId = getConfig("自习室");
        String seatNum = getConfig("座位号");
        return String.format("{\"cardNumber\": \"%s\", \"applyDate\": \"%s\", \"applyStartTime\": \"06:30\", \"applyDuration\": \"15.5\", \"labRoomId\": \"%s\", \"seatNum\": \"%s\"}",
                cardNumber, dateStr, labRoomId, seatNum);
    }
    private static String loadJsonData1(String dateStr) {
        String cardNumber = getConfig("学号");
        String labRoomId1 = getConfig("备选自习室");
        String seatNum1= getConfig("备选座位号");
        return String.format("{\"cardNumber\": \"%s\", \"applyDate\": \"%s\", \"applyStartTime\": \"06:30\", \"applyDuration\": \"15.5\", \"labRoomId\": \"%s\", \"seatNum\": \"%s\"}",
                cardNumber, dateStr, labRoomId1, seatNum1);
    }



    //读取配置文件中的信息
    private static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + File.separator + "config.txt";
    private static String getConfig(String key) {
        try {
            String content = Files.readString(Paths.get(CONFIG_FILE_PATH), StandardCharsets.UTF_8);
            for (String line : content.lines().toList()) {
                if (line.startsWith(key + "=")) {
                    return line.substring(key.length() + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
