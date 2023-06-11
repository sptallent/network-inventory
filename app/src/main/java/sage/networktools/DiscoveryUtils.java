package sage.networktools;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import android.util.Log;

import org.apache.commons.net.util.SubnetUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;
import static com.crashlytics.android.answers.Answers.TAG;

public class DiscoveryUtils {

    private Context context;
    WifiManager wifiManager;
    HashMap<String, String> vendorMap;

    public DiscoveryUtils(Context c) {
        context = c;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        createVendorList();
    }

    public HashMap<String, Integer> grabNetworkSubnets() {
        HashMap<String, Integer> addressList = new HashMap<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (!networkInterface.isLoopback() && networkInterface.getName().equals("wlan0")) {
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress ip = interfaceAddress.getAddress();
                        if (!ip.getHostAddress().contains(":")) { // check if ipv4
                            addressList.put(interfaceAddress.getAddress().toString().substring(1), (int) interfaceAddress.getNetworkPrefixLength());
                        } else {
                            // Currently no IPv6 Support
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return addressList;
    }

    public String getFirstSubnet() {
        HashMap<String, Integer> subnets = grabNetworkSubnets();
        if (subnets.size() > 0) {
            String key = subnets.keySet().toArray()[0].toString();
            return key + "/" + subnets.get(key);
        }
        return null;
    }

    public String[] getAllAddresses(String address, String mask) {
        SubnetUtils subnetUtils = new SubnetUtils(address + "/" + mask);
        return subnetUtils.getInfo().getAllAddresses();
    }

    public boolean isConnected() { //check if connected to a wireless network
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected())
            return true;
        return false;
    }

    public String getNetworkName() {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String name = wifiInfo.getSSID();

        return name;
    }

    public String getMacAddr(InetAddress addr) {
        String macAddress = "";
        try {

            NetworkInterface network = NetworkInterface.getByInetAddress(addr);
            byte[] macArray = network.getHardwareAddress();
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < macArray.length; i++) {
                str.append(String.format("%02X%s", macArray[i], (i < macArray.length - 1) ? " " : ""));
                macAddress = str.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getStackTrace().toString());
        }
        return macAddress;
    }


    public String getWifiMacAddress() {
        /*try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)){
                    continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null){
                    return "00:00:00:0:00:00";
                }
                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "00:00:00:0:00:00";*/
        return wifiManager.getConnectionInfo().getBSSID();
    }

    public Device getUserDevice() {
        String myIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        String myName = Build.MODEL;
        String myMac = getMyMacAddress();//wifiManager.getConnectionInfo().getMacAddress()
        String myVendor = getVendor(myMac);

        Device myDevice = new Device();
        myDevice.setHostName(myName);
        myDevice.setLocalAddress(myIP);
        myDevice.setMac(myMac);
        myDevice.setVendor(myVendor);

        return myDevice;
    }

    public String getMyMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    public String getVendor(String mac) {
        if(mac.equals("") || mac.equals("00:00:00:00:00:00"))
            return "Not Available";
        String small_mac = mac.substring(0, 8).replaceAll("-", ":").toUpperCase();
        String vendor;

        vendor = vendorMap.get(small_mac);

        if (vendor == null) {
            return "NOT AVAILABLE";
        }
        return vendor;
    }

    public String getMask() {
        return intToIP(wifiManager.getDhcpInfo().netmask);
    }

    private String intToIP(int ipAddress) {
        String ret = String.format("%d.%d.%d.%d", (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));

        return ret;
    }

    public void createVendorList() {
        vendorMap = new HashMap<>();
        String vendor;
        String mac;
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("oui.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() >= 10) {
                    mac = line.substring(0, 8);
                    vendor = line.substring(9).trim();
                    vendorMap.put(mac.toUpperCase(), vendor.toUpperCase());
                }
            }
            is.close();
            br.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public Network getNetwork() {
        Network network = new Network();
        network.setMac(getWifiMacAddress());
        network.setSsid(getNetworkName());
        network.setSubnet(getMask());
        return network;
    }

    public int getSignalStrength() {
        int num = 5;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 3);
    }

    public int getDBM() {
        return wifiManager.getConnectionInfo().getRssi();
    }
}
