package com.sipserver.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sipserver.R;
import com.sipserver.model.ClientInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 客户端列表适配器
 */
public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ViewHolder> {
    
    private List<ClientInfo> clients;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    public ClientAdapter(List<ClientInfo> clients) {
        this.clients = clients;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_client, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClientInfo client = clients.get(position);
        
        holder.tvExtension.setText(client.getExtension());
        holder.tvAddress.setText(client.getIpAddress() + ":" + client.getPort());
        holder.tvStatus.setText(getStatusText(client.getStatus()));
        
        String time = timeFormat.format(new Date(client.getLastActiveTime()));
        holder.tvLastActive.setText(time);
        
        holder.tvUserAgent.setText(client.getUserAgent());
    }
    
    private String getStatusText(ClientInfo.RegisterStatus status) {
        switch (status) {
            case ONLINE:
                return "在线";
            case OFFLINE:
                return "离线";
            case BUSY:
                return "忙线";
            case AWAY:
                return "离开";
            default:
                return "未知";
        }
    }
    
    @Override
    public int getItemCount() {
        return clients.size();
    }
    
    public void updateData(List<ClientInfo> newClients) {
        this.clients = newClients;
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExtension;
        TextView tvAddress;
        TextView tvStatus;
        TextView tvLastActive;
        TextView tvUserAgent;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvExtension = itemView.findViewById(R.id.tv_extension);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvLastActive = itemView.findViewById(R.id.tv_last_active);
            tvUserAgent = itemView.findViewById(R.id.tv_user_agent);
        }
    }
}
