package sage.networktools;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.Callable;

import jcifs.netbios.NbtAddress;

import static android.content.ContentValues.TAG;

class NBNSCallable implements Callable<HashMap<String, String>> {

    private String address;
    DatagramSocket datagramSocket;
    InetAddress nbAddress;
    DatagramPacket datagramPacket;
    private byte[] nbNameQueryData = new byte[50];

    public NBNSCallable(String a) {
        address = a;
        try {
            nbAddress = InetAddress.getByName(address);
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(250);
        }catch(SocketException|UnknownHostException e) {
            Log.e(TAG, e.getMessage());
        }
        //build NetBIOS packetData
        nbNameQueryData[0] = -126; // TRANSACTION_ID byte 1
        nbNameQueryData[1] = 40; // TRANSACTION_ID byte 2
        nbNameQueryData[5] = 1; // QUESTION ENTRIES byte 2
        nbNameQueryData[12] = 32;
        nbNameQueryData[13] = 67;
        nbNameQueryData[14] = 75;
        for(int i = 15; i < 45; i++) { // QUESTION NAME 32 bytes
            nbNameQueryData[i] = 65;
        }
        nbNameQueryData[47] = 33; // TYPE is nbstat
        nbNameQueryData[49] = 1; // CLASS is IN
    }

    @Override
    public HashMap<String, String> call() {
        String nbnsName = null;
        HashMap<String, String> tempMap = new HashMap<>();
        try {
            byte[] nbHolderArr = new byte[2048];
            datagramPacket = new DatagramPacket(nbHolderArr, nbHolderArr.length, nbAddress, 137);
            datagramSocket.send(new DatagramPacket(nbNameQueryData, nbNameQueryData.length, nbAddress, 137));
            datagramSocket.receive(datagramPacket);
            byte[] data = datagramPacket.getData();
            if(data != null) {
                byte[] transactionID = new byte[2];
                transactionID[0] = data[0];
                transactionID[1] = data[1];

                byte[] data2 = new byte[16];
                int start = 56;
                int place = 0;
                for(int count = start; count < start + 16; count++) {//populate byte array with computer netbios name
                    data2[place] = data[count];
                    place++;
                }

                nbnsName = new String(data2, 0, 15, "ASCII").trim();
            }
        }catch(Exception ex) {
            //Log.e(TAG, ex.getMessage());
        }

        datagramSocket.close();
        if(nbnsName == null) {
            return null;
        }
        tempMap.put(address, nbnsName);
        return tempMap;
    }

    public boolean pingHost(String host) throws IOException, InterruptedException {
        InetAddress inetAddress = InetAddress.getByName(host);
        return inetAddress.isReachable(3000);
    }
}


