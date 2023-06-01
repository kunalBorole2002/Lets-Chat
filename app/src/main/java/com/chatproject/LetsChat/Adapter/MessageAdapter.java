package com.chatproject.LetsChat.Adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chatproject.LetsChat.EncryptionDecryptionAES;
import com.chatproject.LetsChat.Full_Screen;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.chatproject.LetsChat.Model.Chat;
import com.chatproject.LetsChat.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static  final int MSG_TYPE_LEFT = 0;
    public static  final int MSG_TYPE_RIGHT = 1;
    Typeface MR,MRR;


    private Context mContext;
    private List<Chat> mChat;
    private String imageurl;
    private String userid;

    FirebaseUser fuser;
    DatabaseReference reference;

    public MessageAdapter(Context mContext, List<Chat> mChat, String imageurl, String userid){
        this.mChat = mChat;
        this.mContext = mContext;
        this.imageurl = imageurl;
        this.userid= userid;

        MRR = Typeface.createFromAsset(mContext.getAssets(), "fonts/myriadregular.ttf");
        MR = Typeface.createFromAsset(mContext.getAssets(), "fonts/myriad.ttf");

    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new MessageAdapter.ViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageAdapter.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        final Chat chat = mChat.get(position);
        String type = chat.getType();
        if (chat.getMessage().equals("This message was deleted")) {
            if (userid.equals(chat.getSender())){
                holder.show_message.setPadding(80,12,30,30);
            }
            holder.deleted_icon.setVisibility(View.VISIBLE);
            holder.show_message.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC), Typeface.ITALIC);
        }else if (chat.getMessage().equals("This Image was deleted")) {
            if (chat.getType().equals("image")) {
                if (userid.equals(chat.getSender())) {
                    holder.show_message.setPadding(80, 12, 30, 30);
                } else {
                    holder.show_message.setPadding(70, 12, 90, 30);
                }
                holder.deleted_icon.setVisibility(View.VISIBLE);
                holder.show_message.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC), Typeface.ITALIC);
            }
        } else if (chat.getMessage().equals("This Document was deleted")) {
                if (userid.equals(chat.getSender())){
                    holder.show_message.setPadding(80,12,30,30);
                }else {
                    holder.show_message.setPadding(50, 12, 40, 30);
                }
            holder.deleted_icon.setVisibility(View.VISIBLE);
            holder.show_message.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC), Typeface.ITALIC);

        }else {
            holder.show_message.setTypeface(MRR);
        }
        holder.file_name.setTypeface(MRR);
        holder.txt_seen.setTypeface(MRR);

        if (imageurl.equals("default")) {
            holder.profile_image.setImageResource(R.drawable.profile_img);
        } else {
            Glide.with(mContext).load(imageurl).into(holder.profile_image);
        }

        if (type.equals("text")) {

            EncryptionDecryptionAES ed = new EncryptionDecryptionAES();

            holder.images.setVisibility(View.GONE);
            holder.file_name.setVisibility(View.GONE);

            String message = chat.getMessage();
            String d_msg = null;
            try {
                d_msg = ed.AESDecryptionMethod(message);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            holder.show_message.setText(d_msg);
            holder.show_message.setVisibility(View.VISIBLE);


            if (userid.equals(chat.getReceiver())) {
                if (position == mChat.size() - 1) {
                    if (chat.isIsseen()) {
                        holder.txt_seen.setText("Seen");
                        holder.txt_seen.setVisibility(View.VISIBLE);
                        holder.txt_seen.setText("Seen");
                    } else {
                        holder.txt_seen.setVisibility(View.VISIBLE);
                        holder.txt_seen.setText("Delivered");
                    }
                } else {
                    holder.txt_seen.setVisibility(View.GONE);
                }
            }

            if (chat.getTime() != null && !chat.getTime().trim().equals("")) {
                holder.time_tv.setVisibility(View.VISIBLE);
                holder.time_tv.setText(holder.convertTime(chat.getTime()));
            }


    }else if(type.equals("image")){
            holder.show_message.setVisibility(View.GONE);
            holder.time_tv.setVisibility(View.GONE);
            holder.txt_seen.setVisibility(View.GONE);
            holder.file_name.setVisibility(View.GONE);


            if (chat.getMessage().equals("This Image was deleted") ) {
                holder.show_message.setText(chat.getMessage());
                holder.show_message.setVisibility(View.VISIBLE);
                holder.deleted_icon.setVisibility(View.VISIBLE);
                holder.images.setVisibility(View.GONE);
                if (chat.getTime() != null && !chat.getTime().trim().equals("")) {
                    holder.time_tv.setVisibility(View.VISIBLE);
                    holder.time_tv.setText(holder.convertTime(chat.getTime()));
                }
            }else {

                Picasso.get().load(chat.getMessage()).into(holder.images);
                holder.images.setVisibility(View.VISIBLE);
                Picasso.get().load(chat.getMessage()).into(holder.images);
            }



            holder.images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = chat.getMessage();
                    Intent intent= new Intent(mContext, Full_Screen.class);
                    intent.putExtra("url",url);
                    mContext.startActivity(intent);

                }
            });

        }else {
            holder.show_message.setVisibility(View.GONE);
            holder.time_tv.setVisibility(View.GONE);
            holder.txt_seen.setVisibility(View.GONE);
            holder.file_name.setVisibility(View.VISIBLE);
            holder.images.setVisibility(View.VISIBLE);

            if (chat.getMessage().equals("This Document was deleted") ) {
                holder.show_message.setText(chat.getMessage());
                holder.file_name.setVisibility(View.GONE);
                holder.deleted_icon.setVisibility(View.VISIBLE);
                holder.show_message.setVisibility(View.VISIBLE);
                holder.images.setVisibility(View.GONE);
                if (chat.getTime() != null && !chat.getTime().trim().equals("")) {
                    holder.time_tv.setVisibility(View.VISIBLE);
                    holder.time_tv.setText(holder.convertTime(chat.getTime()));
                }
            }else {

                if (type.equals("pdf")) {
                    holder.images.setBackgroundResource(R.mipmap.pdf);
                    holder.file_name.setText(chat.getFilename());
                    holder.file_name.setVisibility(View.VISIBLE);
                }else{
                    holder.images.setBackgroundResource(R.mipmap.docx);
                    holder.file_name.setText(chat.getFilename());
                    holder.file_name.setVisibility(View.VISIBLE);
                }
            }


            holder.images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(chat.getMessage()));
                    holder.images.getContext().startActivity(intent);
                }
            });
        }
        holder.images.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete this Image?");
                // delete button
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String msgTime = mChat.get(position).getTime();
                        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("Chats");
                        Query query = dbref.orderByChild("time").equalTo(msgTime);
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    if (chat.getType().equals("image")) {

                                        HashMap<String, Object> hashmap = new HashMap<>();
                                        hashmap.put("message", "This Image was deleted");
                                        ds.getRef().updateChildren(hashmap);
                                    }else {
                                        HashMap<String, Object> hashmap = new HashMap<>();
                                        hashmap.put("message", "This Document was deleted");
                                        ds.getRef().updateChildren(hashmap);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                });
                //cancel button
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                    if (userid.equals(chat.getReceiver())) {
                        builder.create().show();
                }


                return false;
            }
        });

        holder.show_message.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete this message?");
                // delete button
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String msgTime = mChat.get(position).getTime();
                        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("Chats");
                        Query query = dbref.orderByChild("time").equalTo(msgTime);
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                                    HashMap<String, Object> hashmap = new HashMap<>();
                                    hashmap.put("message", "This message was deleted");
                                    ds.getRef().updateChildren(hashmap);


                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                });
                //cancel button
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                if (chat.getMessage().equals("This message was deleted")) {
                    holder.show_message.setFocusable(false);
                    holder.show_message.setEnabled(false);
                }else if (chat.getMessage().equals("This Image was deleted")) {
                    holder.show_message.setFocusable(false);
                    holder.show_message.setEnabled(false);
                }
                else if (chat.getMessage().equals("This Document was deleted")) {
                    holder.show_message.setFocusable(false);
                    holder.show_message.setEnabled(false);}
                else {
                    if (userid.equals(chat.getReceiver())) {
                        builder.create().show();
                    }
                }

                return false;
            }
        });

    }


    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public  class ViewHolder extends RecyclerView.ViewHolder{

        public TextView show_message , file_name;
        public ImageView profile_image;
        public TextView txt_seen;
        public TextView time_tv;
        public ImageView images , deleted_icon;
        public ViewHolder(View itemView) {
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            time_tv = itemView.findViewById(R.id.time_tv);
            images = itemView.findViewById(R.id.images);
            deleted_icon = itemView.findViewById(R.id.deleted_icon);
            file_name = itemView.findViewById(R.id.file_name);
        }

        public String convertTime(String time){
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
            String dateString = formatter.format(new Date(Long.parseLong(time)));
            return dateString;
        }
    }

    @Override
    public int getItemViewType(int position) {
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (mChat.get(position).getSender().equals(fuser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}