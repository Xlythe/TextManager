package com.xlythe.sms.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.xlythe.sms.R;
import com.xlythe.textmanager.text.Attachment;

import java.util.List;

/**
 * Created by Niko on 5/13/16.
 */
public class MessageAttachmentAdapter  extends RecyclerView.Adapter<MessageAttachmentAdapter.AttachmentViewHolder> {

    private List<Attachment> mAttachments;
    private Context mContext;

    public MessageAttachmentAdapter(Context context, List<Attachment> attachments) {
        mAttachments = attachments;
        mContext = context;
    }

    public static class AttachmentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView attachment;

        public AttachmentViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            attachment = (ImageView) itemView.findViewById(R.id.attachment);
        }

        @Override
        public void onClick(View view) {

        }
    }

    @Override
    public AttachmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(mContext).inflate(R.layout.list_item_message_attachment, parent, false);
        return new AttachmentViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(AttachmentViewHolder holder, int position) {
       Glide.with(mContext)
                .load(mAttachments.get(position).getUri())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontAnimate()
                .placeholder(R.color.loading)
                .into(holder.attachment);
    }

    @Override
    public int getItemCount() {
        return mAttachments.size();
    }
}
