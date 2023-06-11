package sage.networktools;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomRecyclerAdapter extends RecyclerView.Adapter {

    public interface OnDeviceClickListener {
        void onDeviceClick(String deviceMac);
    }

    Context context;
    ArrayList<Device> deviceList;
    public final OnDeviceClickListener l;

    public CustomRecyclerAdapter(Context c, ArrayList<Device> d, OnDeviceClickListener listener) {
        context = c;
        this.l = listener;
        if (d == null)
            deviceList = new ArrayList<>();
        else
            deviceList = d;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new CustomViewHolder(v, l);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(position < getItemCount() && deviceList.get(position) != null) {
            ((CustomViewHolder) holder).host.setText(deviceList.get(position).getHostName());
            ((CustomViewHolder) holder).ip.setText(deviceList.get(position).getLocalAddress());
            ((CustomViewHolder) holder).vendor.setText(deviceList.get(position).getVendor());
            ((CustomViewHolder) holder).mac.setText(deviceList.get(position).getMac());
        }
    }

    public void onItemDismiss(int position) {
        try {
            deviceList.remove(position);
            notifyItemRemoved(position);
        }catch(IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
}


class CustomViewHolder extends RecyclerView.ViewHolder {

    TextView host;
    TextView ip;
    TextView vendor;
    TextView mac;

    public CustomViewHolder(View itemView, CustomRecyclerAdapter.OnDeviceClickListener listener) {
        super(itemView);

        host = (TextView) itemView.findViewById(R.id.hostname_text);
        ip = (TextView) itemView.findViewById(R.id.ip_text);
        vendor = (TextView) itemView.findViewById(R.id.vendor_text);
        mac = (TextView) itemView.findViewById(R.id.mac_text);

        itemView.setOnClickListener(view -> {
            listener.onDeviceClick(mac.getText().toString());
        });
    }


}
