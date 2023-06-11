package sage.networktools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.clans.fab.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.pedant.SweetAlert.SweetAlertDialog;
import sage.networktools.MulticastDNSDiscover.MulticastDNSPacket;
import sage.networktools.MulticastDNSDiscover.MulticastDNSQuestion;

import static android.content.Context.WIFI_SERVICE;
import static com.crashlytics.android.answers.Answers.TAG;

public class ScanFragment extends Fragment {

    private ScanFragment.OnFragmentInteractionListener mListener;
    ArrayList<Device> device_list;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    FloatingActionButton fabScan;
    CustomRecyclerAdapter recyclerAdapter;
    DiscoveryUtils discoveryUtils;
    boolean scanStatus = false;
    TextView networkSSID;
    TextView networkMac;
    TextView networkVendor;
    TextView networkDeviceCount;
    ImageView networkSignal;
    TextView dbmDisplay;
    DeviceDatabaseHelper databaseHelper;
    ScanAsyncTask scanAsyncTask;

    Network network;
    WifiManager wifiManager;
    WifiManager.MulticastLock multicastLock;

    private HashMap<String, ArrayList<String>> mdnsMap;
    private HashMap<String, String> snmpMap;
    private HashMap<String, HashMap<String, String>> upnpMap;
    private HashMap<String, String> nbnsMap;

    Context context;

    /*private BroadcastReceiver networkReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            network = (Network) intent.getSerializableExtra("network");
            setNetwork();
        }
    };

    private BroadcastReceiver deviceReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean skip = false;
            Device device = (Device) intent.getSerializableExtra("device");
            for(Device d : device_list) {
                if(d.getMac().equals(device.getMac())) {
                    skip = true;
                    break;
                }
            }

            if(!skip) {
                device_list.add(databaseHelper.getDeviceFromNetwork(device, network));
                recyclerAdapter.notifyDataSetChanged();
            }
        }
    };

    private BroadcastReceiver scanStatusReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanComplete(true);
        }
    };*/

    public ScanFragment() {

    }

    public static ScanFragment newInstance(int page, String title) {
        ScanFragment fragment = new ScanFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device_list = new ArrayList<>();

        /*LocalBroadcastManager.getInstance(getContext()).registerReceiver(networkReciever, new IntentFilter("networkFound"));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(deviceReciever, new IntentFilter("deviceFound"));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(scanStatusReciever, new IntentFilter("scanComplete"));*/


        this.mdnsMap = new HashMap<>();
        this.snmpMap = new HashMap<>();
        this.upnpMap = new HashMap<>();
        this.nbnsMap = new HashMap<>();

        wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        if(wifiManager != null) {
            multicastLock = wifiManager.createMulticastLock("multicastLock");
            multicastLock.acquire();
        }

        upnpThread.start();
        mdnsThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        networkSSID = getActivity().findViewById(R.id.network_ssid);
        networkMac = getActivity().findViewById(R.id.network_mac);
        networkVendor = getActivity().findViewById(R.id.network_vendor);
        networkDeviceCount = getActivity().findViewById(R.id.network_device_count);
        networkSignal = getActivity().findViewById(R.id.signal_img);

        discoveryUtils = new DiscoveryUtils(getContext());
        databaseHelper = new DeviceDatabaseHelper(getContext());
        scanAsyncTask = new ScanAsyncTask();
        setNetwork();

        recyclerView = getActivity().findViewById(R.id.reycler_view);
        recyclerAdapter = new CustomRecyclerAdapter(getContext(), device_list, deviceMac -> {

            scanAsyncTask.cancel(true);
            mdnsThread.interrupt();
            upnpThread.interrupt();

            Intent intent = new Intent(context, DeviceActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("device_id", databaseHelper.getDevice(deviceMac).getId());
            intent.putExtras(bundle);
            startActivity(intent);
        });

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(recyclerAdapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                recyclerAdapter.onItemDismiss(viewHolder.getAdapterPosition());
                setNetwork();
            }

        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        fabScan = getActivity().findViewById(R.id.fab_scan);
        fabScan.setOnClickListener(v -> {
            if (scanStatus) {
                scanComplete(true);
            } else {
                if (discoveryUtils.isConnected()) {
                    startScan();
                } else {
                    new SweetAlertDialog(getContext(), SweetAlertDialog.ERROR_TYPE).setTitleText(getString(R.string.warning_title)).setContentText(getString(R.string.warning_text)).show();
                }
            }
        });

        setNetwork();

        swipeRefreshLayout = getActivity().findViewById(R.id.scan_swipe_refresh);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if(!scanStatus)
                startScan();
            swipeRefreshLayout.setRefreshing(false);
        });

        dbmDisplay = getActivity().findViewById(R.id.dbm_network_display);
        dbmDisplay.setText(String.valueOf(discoveryUtils.getDBM()));
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context c) {
        context = c;

        super.onAttach(context);
        if (context instanceof ScanFragment.OnFragmentInteractionListener) {
            mListener = (ScanFragment.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        scanAsyncTask.cancel(true);

        /*if(multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }*/

        upnpThread.interrupt();
        mdnsThread.interrupt();

        super.onDestroy();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void scanComplete(boolean cancelled) {
        scanAsyncTask.cancel(true);
        scanStatus = false;

        setNetwork();

        fabScan.setImageResource(R.drawable.action_scan);
        fabScan.setProgress(0, true);
        fabScan.hideProgress();
        fabScan.show(false);
        if (getContext() != null) {
            if (cancelled) {
                new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE).setTitleText(getString(R.string.scan_stopped)).setContentText("Found " + device_list.size() + " devices on " + discoveryUtils.getNetworkName() + ".").show(); //include network name instead
            } else {
                new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE).setTitleText(getString(R.string.scan_completed)).setContentText("There are " + device_list.size() + " devices on " + discoveryUtils.getNetworkName() + ".").show(); //include network name instead
            }
        }
    }


    public void startScan() {
        device_list.clear();
        setNetwork();
        scanStatus = true;
        fabScan.setImageResource(R.drawable.stop);
        fabScan.setProgress(0, true);

        scanAsyncTask = new ScanAsyncTask();
        scanAsyncTask.execute();
    }

    public void setNetwork() {
        network = discoveryUtils.getNetwork();
        if (databaseHelper.getNetwork(network.getMac()) == null)
            databaseHelper.addNetwork(network);
        network = databaseHelper.getNetwork(network.getMac());

        if (network != null) {
            networkSSID.setText("SSID: " + network.getSsid());
            networkMac.setText("Interface: " + network.getMac().toUpperCase());
            networkVendor.setText(discoveryUtils.getVendor(network.getMac()));

            networkDeviceCount.setText(device_list.size() + " " + getString(R.string.devices));
            int signalStrength = discoveryUtils.getSignalStrength();
            switch (signalStrength) {
                case 0:
                    DrawableCompat.setTint(networkSignal.getDrawable(), ContextCompat.getColor(getContext(), R.color.colorRed));
                    break;
                case 1:
                    DrawableCompat.setTint(networkSignal.getDrawable(), ContextCompat.getColor(getContext(), R.color.colorYellow));
                    break;
                case 2:
                    DrawableCompat.setTint(networkSignal.getDrawable(), ContextCompat.getColor(getContext(), R.color.colorGreen));
                    break;
            }
        }

    }

    public void updateProgress(int max, int cur) {
        fabScan.setMax(max);
        fabScan.setProgress(cur, true);
    }

    public void deviceFound(Device device) {
        boolean skip = false;
        for (Device d : device_list) {
            if (d.getMac().equals(device.getMac())) {
                skip = true;
                break;
            }
        }

        if (!skip) {
            Device d = databaseHelper.getDeviceFromNetwork(device, network);
            if(d != null) {
                d.setVendor(discoveryUtils.getVendor(d.getMac()));
                device_list.add(device);
                recyclerAdapter.notifyDataSetChanged();
            }
        }
    }

    public Device addDevice(Device d) {
        if (databaseHelper.getDevice(d.getMac()) == null) {
            d.setHostName(d.getLocalAddress());
            databaseHelper.addDevice(d);
        }
        d.setId(databaseHelper.getDevice(d.getMac()).getId());

        ArrayList<Device> networkDevices = databaseHelper.getNetworkDevices(network.getId());
        boolean exists = false;
        for (Device de : networkDevices) {
            if (de.getMac().equals(d.getMac()))
                exists = true;
        }
        if (!exists)
            databaseHelper.addDeviceToNetwork(d, network);
        databaseHelper.updateDeviceTime(d.getId());
        return d;
    }

    public class ScanAsyncTask extends AsyncTask {

        private HashMap<String, Integer> addressMap;
        private HashMap<String, String> nbnsMap;
        private ArrayList<String> addressList;
        private ArrayList<String> devices_found;
        private File LOCAL_ARP = new File("/proc/net/arp");
        int countMax;
        int count;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setNetwork();

            addressMap = new HashMap<>();
            devices_found = new ArrayList<>();
            addressList = new ArrayList<>();
            nbnsMap = new HashMap<>();
            countMax = 0;
            count = 0;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            if (values != null) {
                Device device = (Device) values[0];
                /*if(device.getMac().toUpperCase().equals(network.getMac().toUpperCase()))
                    device.setHostName("Default Gateway");
                else*/
                if (device.getHostName().isEmpty())
                    device.setHostName(device.getLocalAddress());
                deviceFound(device);
            }
            updateProgress(countMax, count);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            addressMap = discoveryUtils.grabNetworkSubnets();
            if (!addressMap.isEmpty()) {
                for (HashMap.Entry<String, Integer> entry : this.addressMap.entrySet()) {
                    String address = entry.getKey();
                    Integer mask = entry.getValue();
                    String[] tempAddresses = discoveryUtils.getAllAddresses(address, String.valueOf(mask));
                    Collections.addAll(addressList, tempAddresses);
                }
            }

            /*for(String address : addressList)
                System.out.println(new PingWorker(address));

                Devices no longer reachable using InetAddress due to API 29 restrictions?

                */

            countMax = addressList.size() * 2;
            Device myDevice = discoveryUtils.getUserDevice();
            if(databaseHelper.getDevice(myDevice.getMac()) == null)
                databaseHelper.addDevice(myDevice);

            Device tempDevice = databaseHelper.getDevice(myDevice.getMac());
            if(tempDevice != null) {
                myDevice.setId(tempDevice.getId());
                databaseHelper.addDeviceToNetwork(myDevice, network);
                databaseHelper.updateDeviceTime(myDevice.getId());
                publishProgress(myDevice);
            }

            int threadCount = (Runtime.getRuntime().availableProcessors() * 2);
            if (threadCount < 2)
                threadCount = 2;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            ArrayList<Future<HashMap<String, String>>> futures = new ArrayList<>();
            // NBNS Queries
            for (String address : this.addressList) {
                //Runnable worker = new PingWorker(address);
                //executor.execute(worker);

                Future<HashMap<String, String>> nbnsResult = executor.submit(new NBNSCallable(address));
                futures.add(nbnsResult);

                count++;
                publishProgress(null);
            }
            for (int i = 0; i < futures.size(); i++) {
                count++;
                publishProgress(null);
                try {
                    HashMap<String, String> tempMap = futures.get(i).get();
                    if (tempMap != null)
                        nbnsMap.putAll(futures.get(i).get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            executor.shutdownNow();

            readArpCache();

            for(Device device : databaseHelper.getNetworkDevices(network.getId())) {
                if(nbnsMap.containsKey(device.getLocalAddress())) {//NBNS Map
                    if (nbnsMap.get(device.getLocalAddress()) != null)
                        databaseHelper.addNBNS(device.getId(), nbnsMap.get(device.getLocalAddress()));
                }
                if(mdnsMap.containsKey(device.getLocalAddress())) {//MDNS Map
                    if(mdnsMap.get(device.getLocalAddress()) != null) {
                        for(String service : mdnsMap.get(device.getLocalAddress()))
                            databaseHelper.addBonjour(device.getId(), service);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            for(int i = 0; i < device_list.size(); i++) {
                Device device = databaseHelper.getDevice(device_list.get(i).getId());
                if(!databaseHelper.getNBNS(device.getId()).isEmpty()) {
                    device.setHostName(databaseHelper.getNBNS(device.getId()).get(0));
                    device.setLocalAddress(databaseHelper.getDeviceFromNetwork(device, network).getLocalAddress());
                    device.setVendor(discoveryUtils.getVendor(device.getMac()));
                    device_list.remove(i);
                    device_list.add(i, device);
                    recyclerAdapter.notifyDataSetChanged();
                    continue;
                }
                if(!databaseHelper.getBonjour(device.getId()).isEmpty()) {
                    device.setHostName(databaseHelper.getBonjour(device.getId()).get(0));
                    device.setLocalAddress(databaseHelper.getDeviceFromNetwork(device, network).getLocalAddress());
                    device.setVendor(discoveryUtils.getVendor(device.getMac()));
                    device_list.remove(i);
                    device_list.add(i, device);
                    recyclerAdapter.notifyDataSetChanged();
                }
            }

            scanComplete(true);
        }

        public void readArpCache() {
            if(Build.VERSION.SDK_INT >= 30) {

            }else if (Build.VERSION.SDK_INT < 29) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(LOCAL_ARP));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");
                        if (splitted.length >= 4) {
                            String ip = splitted[0];
                            String flags = splitted[2];
                            String mac = splitted[3].toUpperCase();
                            if (!flags.equals("0x2"))
                                continue;

                            Device device = new Device();
                            String vendor = discoveryUtils.getVendor(mac);

                            device.setLocalAddress(ip);
                            device.setMac(mac);
                            device.setVendor(vendor);

                            addDevice(device);
                            publishProgress(device);
                        }
                    }
                    br.close();
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            } else { //no access to /proc/net/arp
                BufferedReader br;
                try {
                    Process ipProc = Runtime.getRuntime().exec("ip neighbor");
                    ipProc.waitFor();
                    if (ipProc.exitValue() != 0) {
                        System.out.println("Unable to access ARP information");
                    }
                    br = new BufferedReader(new InputStreamReader(ipProc.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] neighborLine = line.split("\\s+");
                        if (neighborLine.length <= 4) {
                            continue;
                        }
                        String ip = neighborLine[0];

                        InetAddress addr = InetAddress.getByName(ip);
                        if (addr.isLinkLocalAddress() || addr.isLoopbackAddress()) {
                            continue;
                        }
                        String macAddress = neighborLine[4];

                        Device device = new Device();
                        device.setLocalAddress(ip);
                        device.setMac(macAddress.toUpperCase());
                        device.setVendor(discoveryUtils.getVendor(macAddress));
                        device = addDevice(device);
                        publishProgress(device);
                    }
                }catch(Exception e) {
                    Log.e(TAG, e.getStackTrace().toString());
                }
            }
        }
    }

    //UPNP
    Thread upnpThread = new Thread(() -> {
        try {
            String group = "239.255.255.250";
            int port = 1900;
            String ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" + "HOST: 239.255.255.250:1900\r\n" +"MAN: \"ssdp:discover\"\r\n" + "MX: 1\r\n" + "ST: upnp:rootdevice\r\n" + "\r\n";
            ArrayList<HashMap<String, String>> profilesList = new ArrayList<>();
            HashMap<String, String> profileMap = new HashMap<>();
            MulticastSocket upnpSocket = new MulticastSocket(port);
            byte[] ssdpHolder = new byte[1024];
            InetAddress address = InetAddress.getByName(group);
            upnpSocket.joinGroup(address);
            upnpSocket.send(new DatagramPacket(ssdpQuery.getBytes(), ssdpQuery.length(), address, port));
            while(true) {
                DatagramPacket ssdpPacket = new DatagramPacket(ssdpHolder, ssdpHolder.length);
                upnpSocket.receive(ssdpPacket);
                byte[] data = ssdpPacket.getData();
                if(data != null) {
                    String serviceInfo = new String(data, "ASCII");
                    if(serviceInfo.contains("HTTP/1.1 200")) {
                        BufferedReader br = new BufferedReader(new StringReader(serviceInfo));
                        String line;
                        while((line = br.readLine()) != null) {
                            if(line.contains(": ")) {
                                String key = line.substring(0, line.indexOf(": ")).trim();
                                String value = line.substring(line.indexOf(": ") + 2).trim();
                                profileMap.put(key, value);
                            }
                        }
                        if(profileMap.get("LOCATION") != null && profileMap.get("USN") != null) {
                            String upnpUSN = profileMap.get("USN");
                            String upnpLocation = profileMap.get("LOCATION");
                            String ipRegex = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})";
                            Pattern ipPattern = Pattern.compile(ipRegex);
                            Matcher ipMatcher = ipPattern.matcher(upnpLocation);
                            if (ipMatcher.find()) {
                                boolean skip = false;
                                for (HashMap<String, String> tempMap : profilesList) {
                                    if (tempMap.get("USN").equals(upnpUSN)) {
                                        skip = true;
                                        break;
                                    }
                                }
                                if (!skip) {
                                    profilesList.add(profileMap);
                                    upnpMap.put(upnpUSN, profileMap);
                                    Log.e(TAG, "UPNP: " + upnpMap.toString());
                                }
                            }
                        }
                        profileMap = new HashMap<>();
                    }
                }
            }
            //upnpSocket.leaveGroup(InetAddress.getByName(group));
            //upnpSocket.close();
        }catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }
    });

    //mDNS
    Thread mdnsThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                String group = "224.0.0.251";
                int port = 5353;
                ArrayList<String> serviceList = new ArrayList<>();
                byte[] mdnsQuery = new MulticastDNSQuestion("_services._dns-sd._udp.local").getBytes();
                MulticastSocket mdnsSocket = new MulticastSocket(port);
                byte[] mdnsHolder = new byte[1024];
                InetAddress address = InetAddress.getByName(group);
                mdnsSocket.joinGroup(address);
                mdnsSocket.send(new DatagramPacket(mdnsQuery, mdnsQuery.length, address, port));
                while (true) {
                    DatagramPacket mdnsPacket = new DatagramPacket(mdnsHolder, mdnsHolder.length);
                    mdnsSocket.receive(mdnsPacket);
                    byte[] data = mdnsPacket.getData();
                    if (data != null) {
                        if (((data[2] >> 15) & 1) == 1) {
                            MulticastDNSPacket multicastDNSPacket = new MulticastDNSPacket(data);
                            String deviceIP = mdnsPacket.getAddress().getHostAddress();
                            for (String serviceName : multicastDNSPacket.getServiceNames()) {
                                if (!serviceList.contains(serviceName)) {
                                    serviceList.add(serviceName);
                                }
                            }
                            ArrayList<String> deviceNames = new ArrayList<>();
                            for (String deviceName : multicastDNSPacket.getDeviceNames()) {
                                if (!deviceNames.contains(deviceName) && !deviceName.equals("localhost"))
                                    deviceNames.add(deviceName.replace(".local", ""));
                            }
                            if(!deviceNames.isEmpty() & (mdnsMap.get(deviceIP) == null || mdnsMap.get(deviceIP).size() < deviceNames.size())) {
                                mdnsMap.put(deviceIP, deviceNames);
                            }
                            if(mdnsMap.get(deviceIP) == null) {
                                String reverseIP = "";
                                byte[] deviceAddress = mdnsPacket.getAddress().getAddress();
                                for (int i = deviceAddress.length - 1; i >= 0; i--) {
                                    reverseIP += (deviceAddress[i] & 0xFF);
                                    reverseIP += ".";
                                }
                                byte[] deviceQuery = new MulticastDNSQuestion(reverseIP + "in-addr.arpa").getBytes();
                                mdnsSocket.send(new DatagramPacket(deviceQuery, deviceQuery.length, address, port));
                            }
                        }
                    }
                }
                //mdnsSocket.leaveGroup(address);
                //mdnsSocket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    });

}
