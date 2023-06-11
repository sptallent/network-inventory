package sage.networktools;

import java.io.IOException;
import java.net.InetAddress;

public class PingWorker implements Runnable {

    private String address;

    public PingWorker(String a) {
        address = a;
    }

    @Override
    public void run() {
        try {
            if(pingHost(address)) {
                System.out.println(address);
            }
        }catch(InterruptedException|IOException e) {
            e.printStackTrace();
        }
    }

    public boolean pingHost(String host) throws IOException, InterruptedException {
        InetAddress inetAddress = InetAddress.getByName(host);
        return inetAddress.isReachable(3000);

        /*String cmd = "/system/bin/ping -c 1 -W 1000 " + host;
        Process proc = Runtime.getRuntime().exec(cmd);
        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String line;*/

        /*while ((line = input.readLine()) != null) {
            System.out.println(line);
        }*/

        /*int exitValue = proc.waitFor();
        proc.destroy();

        input.close();*/
        //return exitValue;
    }
}
