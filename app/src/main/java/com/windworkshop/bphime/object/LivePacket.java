package com.windworkshop.bphime.object;

import com.apkfuns.logutils.LogUtils;
import com.windworkshop.bphime.MainModule;
import com.windworkshop.bphime.activity.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 封包处理类
 */
public class LivePacket {
    int packetLength = 0;
    int headerLength = 16; //  默认封包头为16长度
    int protocolVersion = 1;
    int packetType = 0;
    int sequence = 1;
    String packetData = "";

    private LivePacket() {

    }

    /**
     * 从Buffer中初始化封包
     * @param buffer ByteBuffer数据
     * @deprecated
     */
    public LivePacket(ByteBuffer buffer) {
        // 初始化封包
        packetLength = buffer.getInt(0); // 第一个0-3位数据为总封包长数据
        int nums = buffer.getInt(4); // 第二个4-7位数据为需要分类的两个数据
        headerLength = nums >> 16; // 高两位（前16bit）为头部长度
        protocolVersion = (nums << 16) >> 16; // 反复横跳获得低两位（后16bit）控制版本号
        packetType = buffer.getInt(8); // 第三个8-11为封包类型
        sequence = buffer.getInt(12); // 第四个12-16为sequence

        LogUtils.i( "packetData 包数据raw : " + new String(buffer.array()));

        if(protocolVersion == 2) {
            byte[] rawByteData = buffer.array();
            byte[] realByteData = new byte[rawByteData.length - 16];
            for(int i = 0;i < realByteData.length;i++){
                realByteData[i] = rawByteData[16+ i];
            }

            try {
                String zipString = new String(MainModule.uncompress(realByteData));
                ArrayList<JSONObject> dataArray = new ArrayList<JSONObject>();
                int leftCount = 0;
                int startIndex = 0;
                boolean inString = false;
                for(int i = 0;i < zipString.length();i++) {
                    char c = zipString.charAt(i);
                    if(leftCount <= 0) {
                        if(c == '{') {
                            startIndex = i;
                            leftCount += 1;
                        }
                    } else {
                        if(c == '{') {
                            leftCount += 1;
                        } else if(c == '}') {
                            leftCount -= 1;
                            if(leftCount == 0) {
                                String jsonString = zipString.substring(startIndex, i+1);
                                LogUtils.i("find json:" + jsonString);
                                dataArray.add(new JSONObject(jsonString));
                            }
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        } else {
            packetData = "";
            if(packetLength > headerLength) {
                String dataString = new String(buffer.array()); // 把所有buffer转换为String
                if(dataString.indexOf("{") != -1 && dataString.indexOf("}")!= -1) {
                    // 挑出json格式的数据
                    dataString = dataString.substring(dataString.indexOf("{"), dataString.lastIndexOf("}")+1);
                    // 保存数据
                    packetData = dataString;
                }
                LogUtils.i( "packetData 包数据 : " + packetData);
            }
        }
    }
    // 创建封包基础方法
    public static LivePacket createPacket(PacketType type) {
        LivePacket packet = new LivePacket();
        packet.packetType = type.id;
        return packet;
    }
    // 创建认证包
    public static LivePacket createAuthPacket(String dataJson) {
        LivePacket packet = createPacket(PacketType.JOIN_ROOM); // 设置封包类型
        packet.packetData = dataJson;
        return packet;
    }

    /**
     * 发送前需要将封包转换为Buffer数据
     * @return 返回需要发送的封包
     */
    public ByteBuffer toBuffer() {
        // 总包长
        packetLength = headerLength + packetData.length();
        // 创建封包缓冲
        ByteBuffer bf =  ByteBuffer.allocate(packetLength);
        //ByteBuffer的一个Int（Byte）为四位8bit数据。

        // 第一个Int装入总封包长数据
        bf.putInt(0, packetLength);
        // 第二个Int因为要拆成两个2x8bit来分别储存头部长度和控制版本号，因为放char和byte转换非常麻烦，直接偏移16位处理还快些
        int nums = 0;
        nums = (nums << 16) | headerLength; // 头部长度
        nums = (nums << 16) | protocolVersion; // 控制版本号
        bf.putInt(4,nums); // 装入头部长度和控制版本号数据
        bf.putInt(8,packetType); // 装入封包类型
        bf.putInt(12,sequence); // 装入sequence（不知道这是干嘛的）
        // 有数据的话在一个byte一个byte的装入
        if(packetData.length() > 0) {
            byte[] dataBytes = packetData.getBytes();
            for(int i = 0;i < dataBytes.length;i++) {
                bf.put(16+i,dataBytes[i]);
            }
        }
        return bf;
    }
}