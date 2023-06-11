package sage.networktools;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Device implements Serializable {

    int id;
    private String localAddress = "";
    private String mac = "";
    private String hostname = "";
    private String vendor = "";
    private String timestamp;
    String upnpName = "";
    private ArrayList<HashMap<String, HashMap<String, String>>> upnpProfiles;

    public Device() {
        upnpProfiles = new ArrayList<>();
        this.timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(new Date());
    }

    public String getHostName() {
        return hostname;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public String getMac() {
        return mac;
    }

    public String getVendor() {
        return vendor;
    }

    public void setHostName(String hostname) {
        this.hostname = hostname;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setLocalAddress(String local_ip) {
        this.localAddress = local_ip;
    }

    public ArrayList<HashMap<String, HashMap<String, String>>> getUpnpProfiles() {
        return upnpProfiles;
    }

    public void addUpnpProfile(HashMap<String, HashMap<String, String>> profile) {
        upnpProfiles.add(profile);
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
