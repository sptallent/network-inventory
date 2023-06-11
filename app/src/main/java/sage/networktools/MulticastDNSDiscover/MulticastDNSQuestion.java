package sage.networktools.MulticastDNSDiscover;

/* Change to (ONLY) a question to be added onto a MulticastDNSPacket outgoing
   instead of the full mDNS packet.
 */

public class MulticastDNSQuestion {

    private String questionName;
    private byte questionType;
    private byte questionClass;

    public MulticastDNSQuestion() {

    }

    public MulticastDNSQuestion(String name) {
        questionName = name;
    }

    public MulticastDNSQuestion(String name, byte type, byte clazz) {
        questionName = name;
        questionType = type;
        questionClass = clazz;
    }

    public MulticastDNSQuestion(String name, int type, int clazz) {
        questionName = name;
        questionType = (byte) type;
        questionClass = (byte) clazz ;
    }

    public String getQuestionName() {
        return questionName;
    }

    public byte getQuestionType() {
        return questionType;
    }

    public byte getQuestionClass() {
        return questionClass;
    }

    public void setQuestionName(String questionName) {
        this.questionName = questionName;
    }

    public void setQuestionType(byte questionType) {
        this.questionType = questionType;
    }
    public void setQuestionClass(byte questionClass) {
        this.questionClass = questionClass;
    }

    public byte[] getBytes() {
        String[] name = questionName.split("\\.");
        int nameLen = 0;
        for(String n : name) {
            nameLen += n.length();
        }
        nameLen += name.length;
        byte[] data = new byte[12+nameLen+5];
        data[0] = 0;
        data[1] = 0;
        data[2] = 0;
        data[3] = 0;
        data[4] = 0;
        data[5] = 1;
        data[6] = 0;
        data[7] = 0;
        data[8] = 0;
        data[9] = 0;
        data[10] = 0;
        data[11] = 0;
        int count = 12;
        for(String str : name) {
            byte[] bytes = str.getBytes();
            data[count] = (byte) bytes.length;
            count++;
            for(byte b : bytes) {
                data[count] = b;
                count++;
            }
        }
        data[12+nameLen] = 0;// variable name terminator byte
        data[12+nameLen+1] = 0;
        data[12+nameLen+2] = 12;
        data[12+nameLen+3] = 0;
        data[12+nameLen+4] = 1;
        return data;
    }
}
