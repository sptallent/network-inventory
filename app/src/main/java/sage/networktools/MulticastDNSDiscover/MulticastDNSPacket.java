package sage.networktools.MulticastDNSDiscover;

import android.util.Log;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

public class MulticastDNSPacket {

    private byte[] packetData;
    private int transactionID;
    private int flags;
    private int questionCount;
    private int answerCount;
    private int authorityCount;
    private int additionalCount;
    private ArrayList<String> serviceNames;
    private ArrayList<String> deviceNames;

    public MulticastDNSPacket() {

    }

    public MulticastDNSPacket(DatagramPacket dp) {
        this.packetData = dp.getData();
        parsePacket();
    }

    public MulticastDNSPacket(byte[] tempData) {
        this.packetData = tempData;
        parsePacket();
    }

    public void setPacket(DatagramPacket dp) {
        this.packetData = dp.getData();
        parsePacket();
    }

    public void setPacket(byte[] data) {
        this.packetData = data;
        parsePacket();
    }

    public void parsePacket() {
        transactionID = ((packetData[0] & 0xFF) << 8) | (packetData[1] & 0xFF);
        flags = ((packetData[2] & 0xFF) << 8) | (packetData[3] & 0xFF);
        questionCount = ((packetData[4] & 0xFF) << 8) | (packetData[5] & 0xFF);
        answerCount = ((packetData[6] & 0xFF) << 8) | (packetData[7] & 0xFF);
        authorityCount = ((packetData[8] & 0xFF) << 8) | (packetData[9] & 0xFF);
        additionalCount = ((packetData[10] & 0xFF) << 8) | (packetData[11] & 0xFF);

        int offset = 12;
        if (questionCount > 0) {
            int questionsSkipped = 0;
            while (questionsSkipped < questionCount) {
                //Skip all of the questions. Not important right now.
                while (packetData[offset] != 0x00 && offset < packetData.length) {
                    if ((packetData[offset] & 0xC0) == 0xC0) {
                        offset += 2;
                        break;
                    } else {
                        offset += (packetData[offset]);
                        offset++;
                    }
                }
                offset += 4; // qtype(2), qclass(2)
                questionsSkipped++;
            }

        }

        if (answerCount > 0) {
            serviceNames = new ArrayList<>();
            deviceNames = new ArrayList<>();
            int answersParsed = 0;
            while (answersParsed < answerCount) {
                while (packetData[offset] != 0x00 && offset < packetData.length) {
                    if ((packetData[offset] & 0xC0) == 0xC0) {
                        offset += 2;
                        break;
                    } else {
                        offset += (packetData[offset] & 0xFF);
                        offset++;
                        if(packetData[offset] == 0x00) {
                            offset++;
                            break;
                        }
                    }
                }
                int typeCodes = ((packetData[offset] & 0xFF) << 8) | (packetData[offset + 1] & 0xFF);
                offset += 2;
                int answerClass = ((packetData[offset] & 0xFF) << 8) | (packetData[offset + 1] & 0xFF);
                offset += 2;
                int timeToLive = ((packetData[offset] & 0xFF) << 24) | ((packetData[offset + 1] & 0xFF) << 16) | ((packetData[offset + 2] & 0xFF) << 8) | (packetData[offset + 3] & 0xFF);
                offset += 4;
                int rDataLen = ((packetData[offset] & 0xFF) << 8) | (packetData[offset + 1] & 0xFF);
                offset += 2;
                String rData = "";
                while(packetData[offset] != 0x00 && offset <= offset+rDataLen) {
                    if ((packetData[offset] & 0xC0) == 0xC0) {
                        getPointer(offset);
                        offset += 2;
                        break;
                    } else {
                        rData += getLabel(offset);
                        offset += (packetData[offset] & 0xFF);
                        offset++;
                        if(packetData[offset] == 0x00) {
                            offset++;
                            break;
                        }
                    }
                }

                String serviceRegex = "(_.+(_tcp|_udp).+)";
                String deviceRegex = "(.+.local)";
                Pattern servicePattern = Pattern.compile(serviceRegex);
                Pattern devicePattern = Pattern.compile(deviceRegex);
                Matcher serviceMatcher = servicePattern.matcher(rData);
                Matcher deviceMatcher = devicePattern.matcher(rData);
                if(serviceMatcher.find()) {
                    serviceNames.add(serviceMatcher.group());
                }else if(deviceMatcher.find()) {
                    deviceNames.add(deviceMatcher.group());
                }
                answersParsed++;
            }
        }
    }

    private String getPointer(int offset) {
        String data = "";
        int pointerBitsRemoved = (((packetData[offset]) << 2) >> 2) & 0xFF;
        offset = ((pointerBitsRemoved << 8) & 0xFF) | (packetData[offset + 1] & 0xFF);
        if ((packetData[offset] & 0xC0) == 0xC0) {
            getPointer(offset);
        } else {
            data += getLabel(offset);
        }
        return data;
    }

    private String getLabel(int offset) {
        String data = "";
            if ((packetData[offset] & 0xC0) == 0xC0) {
                getPointer(offset);
            } else {
                int labelLength = (packetData[offset] & 0xFF);
                for (int i = 1; i <= labelLength; i++) {
                    data += new String(new byte[]{packetData[offset + i]});
                }
                data += ".";
                offset += labelLength;
                offset++;
                if(packetData[offset] == 0x00) {
                    return data;
                }else if((packetData[offset] & 0xC0) == 0xC0) {
                    data += getPointer(offset);
                }
            }
        return data;
    }

    public byte[] getPacketData() {
        return packetData;
    }

    public int getTransactionID() {
        return transactionID;
    }

    public int getFlags() {
        return flags;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public int getAuthorityCount() {
        return authorityCount;
    }

    public int getAdditionalCount() {
        return additionalCount;
    }

    public ArrayList<String> getServiceNames() {
        return serviceNames;
    }

    public ArrayList<String> getDeviceNames() {
        return deviceNames;
    }
}
