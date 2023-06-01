package com.chatproject.LetsChat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.chatproject.LetsChat.Adapter.MessageAdapter;
import com.chatproject.LetsChat.Fragments.APIService;
import com.chatproject.LetsChat.Model.Chat;
import com.chatproject.LetsChat.Model.User;
import com.chatproject.LetsChat.Notifications.Client;
import com.chatproject.LetsChat.Notifications.Data;
import com.chatproject.LetsChat.Notifications.MyResponse;
import com.chatproject.LetsChat.Notifications.Token;
import com.chatproject.LetsChat.Notifications.Sender;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username,requirement_tv;

    RelativeLayout requirement_layout;
    Typeface MR, MRR;

    FirebaseUser fuser;
    DatabaseReference reference;

    ImageButton btn_send, send_files;
    EditText text_send;

    MessageAdapter messageAdapter;
    List<Chat> mchat;

    RecyclerView recyclerView;

    Intent intent;

    ValueEventListener seenListener;

    String userid;

    String displayName = null;

    APIService apiService;

    private String checker="", myUrl="";
    private StorageTask uploadTask;
    private Uri fileuri;

    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        MRR = Typeface.createFromAsset(getAssets(), "fonts/myriadregular.ttf");
        MR = Typeface.createFromAsset(getAssets(), "fonts/myriad.ttf");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // and this
                startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);
        requirement_tv = findViewById(R.id.requirement_tv);
        send_files = findViewById(R.id.send_files);
        requirement_layout = findViewById(R.id.requirement_layout);

        username.setTypeface(MR);
        text_send.setTypeface(MRR);


        intent = getIntent();
        userid = intent.getStringExtra("userid");
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notify = true;
                String msg = text_send.getText().toString();
                String time = String.valueOf(System.currentTimeMillis());
                if (!msg.equals("")){
                    try {
                        sendMessage(fuser.getUid(), userid, msg, time);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
                }
                text_send.setText("");
            }
        });
        send_files.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence options[]= new CharSequence[]{
                  "Images",
                  "PDF Files",
                  "Ms Word Files"
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
                builder.setTitle("Select Files");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        if (i== 0){
                            checker="image";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent,"Select Image"), 438);
                        }
                        if (i== 1){
                            checker="pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent,"Select PDF File"), 438);

                        }
                        if (i== 2){
                            checker="docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/docx/*");
                            startActivityForResult(intent.createChooser(intent,"Select MSword File"), 438);

                        }
                    }
                });
                builder.show();
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                String check = user.getRequirement();

                if (check.isEmpty()){
                    requirement_layout.setVisibility(View.GONE);
                } else {
                    requirement_layout.setVisibility(View.VISIBLE);
                    requirement_tv.setText(user.getRequirement());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        requirement_tv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                requirement_layout.setVisibility(View.GONE);
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")){
                    profile_image.setImageResource(R.drawable.profile_img);
                } else {
                    //and this
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }

                readMesagges(fuser.getUid(), userid, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        seenMessage(userid);
    }

    private void seenMessage(final String userid){
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(userid)){
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        snapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==438 && resultCode==RESULT_OK &&data != null && data.getData() !=null){
            final ProgressDialog pd = new ProgressDialog(this);
            pd.setIndeterminateDrawable(getResources().getDrawable(R.drawable.ic_picture));

            pd.setMessage("Sending...");
            pd.show();

            fileuri = data.getData();

            String uriString = fileuri.toString();
            File myFile = new File(uriString);
            String path = myFile.getAbsolutePath();

            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = getApplicationContext().getContentResolver().query(fileuri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else if (uriString.startsWith("file://")) {
                displayName = myFile.getName();
            }


            if (!checker.equals("image")){

                final Chat chat = new Chat();
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Documents Files");

                DatabaseReference userMessageKeyRef = FirebaseDatabase.getInstance().getReference("Chats").child("message").push();

                final String messagePushID = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushID  + "." +checker );


                filePath.putFile(fileuri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                       if(task.isSuccessful()){

                           final String time = String.valueOf(System.currentTimeMillis());

                           final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                           filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                               @Override
                               public void onSuccess(Uri uri) {

                                   final String downloadUrl = uri.toString();
                                   HashMap<String, Object> hashMap = new HashMap<>();
                                   hashMap.put("message", downloadUrl);
                                   hashMap.put("name", fileuri.getLastPathSegment());
                                   hashMap.put("type", checker);
                                   hashMap.put("messageID", messagePushID);
                                   hashMap.put("isseen", false);
                                   hashMap.put("sender", fuser.getUid());
                                   hashMap.put("receiver", userid);
                                   hashMap.put("time", time);
                                   hashMap.put("filename",displayName);

                                   reference.child("Chats").push().setValue(hashMap);
                                   reference.updateChildren(hashMap);
                                   pd.dismiss();
                               }
                           });


                       }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(MessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double p = (100.0*taskSnapshot.getBytesTransferred())/ taskSnapshot.getTotalByteCount();
                        pd.setMessage((int) p + "%   Sending....");
                    }
                });


            }else if (checker.equals("image")){
                final Chat chat = new Chat();
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");
                String messageSenderRef = "Messages/" + chat.getSender() +"/" + chat.getReceiver();
                String messageReceiverRef = "Messages/" + chat.getReceiver() +"/" + chat.getSender();

                DatabaseReference userMessageKeyRef = FirebaseDatabase.getInstance().getReference("Chats").child("message").push();

                final String messagePushID = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushID  + "." + "jpg");

                uploadTask = filePath.putFile(fileuri);
                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()){
                            throw task.getException();
                        }
                        return  filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()){
                            String time = String.valueOf(System.currentTimeMillis());

                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                            Uri doenloadurl = task.getResult();
                            myUrl = doenloadurl.toString();

                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("message", myUrl);
                            hashMap.put("name", fileuri.getLastPathSegment());
                            hashMap.put("type", checker);
                            hashMap.put("messageID", messagePushID);
                            hashMap.put("isseen", false);
                            hashMap.put("sender", fuser.getUid());
                            hashMap.put("receiver", userid);
                            hashMap.put("time", time);

                            reference.child("Chats").push().setValue(hashMap);

                            reference.updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        pd.dismiss();
                                    }
                                }
                            });
                        }
                    }
                });
            }else {
                pd.dismiss();
                Toast.makeText(this,"Nothing Selected, Error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMessage(String sender, final String receiver, String message, String time) throws UnsupportedEncodingException {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();


        EncryptionDecryptionAES ed = new EncryptionDecryptionAES();
        String e_msg = ed.AESEncryptionMethod(message);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", e_msg);
        hashMap.put("isseen", false);
        hashMap.put("time", time);
        hashMap.put("messageID", "");
        hashMap.put("type", "text");

        reference.child("Chats").push().setValue(hashMap);


        // add user to chat fragment
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fuser.getUid())
                .child(userid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    chatRef.child("id").setValue(userid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(userid)
                .child(fuser.getUid());
        chatRefReceiver.child("id").setValue(fuser.getUid());

        final String msg = message;

        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) {
                    sendNotifiaction(receiver, user.getUsername(), msg);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotifiaction(String receiver, final String username, final String message){
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(fuser.getUid(), R.drawable.profile_img, username+": "+message, "New Message",
                            userid);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().success != 1){
                                            //Toast.makeText(MessageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMesagges(final String myid, final String userid, final String imageurl){
        mchat = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mchat.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(myid)){
                        mchat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mchat, imageurl,userid);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void currentUser(String userid){
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userid);
        editor.apply();
    }

    private void status(String status){
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
        currentUser(userid);
    }
    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        status("offline");
        currentUser("none");
    }
}
