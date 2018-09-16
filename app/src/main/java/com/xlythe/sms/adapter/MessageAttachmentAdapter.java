package com.xlythe.sms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.xlythe.sms.R;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.view.camera.Image;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Niko on 5/13/16.
 */
public class MessageAttachmentAdapter  extends RecyclerView.Adapter<MessageAttachmentAdapter.AttachmentViewHolder> {

    private List<Attachment> mAttachments;
    private Context mContext;
    private OnClickListener mOnClickListener;

    public MessageAttachmentAdapter(Context context, List<Attachment> attachments) {
        mAttachments = attachments;
        mContext = context;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public static class AttachmentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mAttachmentView;
        private ImageView videoLabel;
        private OnClickListener mOnClickListener;
        private Attachment mAttachment;

        public AttachmentViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mAttachmentView = itemView.findViewById(R.id.attachment);
            videoLabel = itemView.findViewById(R.id.video_label);
        }

        public void setAttachment(Context context, Attachment attachment, OnClickListener onClickListener) {
            mOnClickListener = onClickListener;
            mAttachment = attachment;
            if (attachment.getType() == Attachment.Type.VIDEO) {
                videoLabel.setVisibility(View.VISIBLE);
            } else {
                videoLabel.setVisibility(View.GONE);
            }
            Glide.with(context)
                    .load(attachment.getUri())
                    .into(mAttachmentView);
        }

        public OnClickListener getOnClickListener() {
            return mOnClickListener;
        }

        @Override
        public void onClick(View view) {
            getOnClickListener().onClick(mAttachment);
        }
    }

    @Override
    public AttachmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(mContext).inflate(R.layout.list_item_message_attachment, parent, false);
        return new AttachmentViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(AttachmentViewHolder holder, int position) {
        holder.setAttachment(mContext, mAttachments.get(position), mOnClickListener);
    }

    @Override
    public int getItemCount() {
        return mAttachments.size();
    }

    public interface OnClickListener {
        void onClick(Attachment attachment);
    }
}
