package com.astro.android.astro.chat;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.astro.android.astro.MainActivity;
import com.astro.android.astro.R;
import com.astro.android.astro.fragment.AccountFragment;
import com.astro.android.astro.model.ChatModel;
import com.astro.android.astro.model.NotifyModel;
import com.astro.android.astro.model.UserModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {

    private String targetUid;
    private Button button;
    private EditText editText;
    private TextView msg_name;
    private ImageView msg_profile;
    private ImageView back;
    private String myUid;
    private String chatRoomUid;
    private ChatModel.LastComment lastCo;
    private LinearLayout account;
    private BottomNavigationView bottom;

    private UserModel receiver;

    private RecyclerView recyclerView;

    //시간
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm aa");

    //back에도 계속 실행되는 이벤트 멈추기
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    private DatabaseReference databaseRef;
    private ValueEventListener valueEventL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        //상단 상태바, 하단 네비게이션바 반투명
        getWindow().setStatusBarColor(Color.parseColor("#20111111"));
        getWindow().setNavigationBarColor(Color.parseColor("#20111111"));

        //변수 정의
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();     //채팅 발신자 uid 현재 로그인
        targetUid = getIntent().getStringExtra("targetUid");     //채팅 수신자   uid
        button = findViewById(R.id.msgactivity_btn_send);
        editText = findViewById(R.id.msgactivity_editText);
        recyclerView = findViewById(R.id.msgactivity_recyclerView);
        msg_profile = findViewById(R.id.msgactivity_profile);
        msg_name = findViewById(R.id.msgactivity_name);
        back = findViewById(R.id.msgactivity_back);
        account = findViewById(R.id.msgactivity_account);
        bottom = findViewById(R.id.mainactivity_bottom_navi);

        //뒤로가기
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //상대방 클릭시
        account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                intent.putExtra("gotoAccount","true");
                intent.putExtra("targetUid",targetUid);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //현재까지 열린 인텐트 다지움
                startActivity(intent);
            }
        });

        //상단에 수신자 정보
        FirebaseDatabase.getInstance().getReference().child("users").child(targetUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                receiver = dataSnapshot.getValue(UserModel.class);
                Glide.with(getApplicationContext())
                        .load(receiver.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(msg_profile);
                msg_name.setText(receiver.name);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //보내기
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //현재 채팅 소속 유저 정의
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(myUid, true);
                chatModel.users.put(targetUid, true);

                //채팅 내용 정의

                //채팅방이 생성된적 없는 경우
                if (chatRoomUid == null) {
                    //연속 클릭 방지
                    button.setEnabled(false);

                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom(true);
                        }
                    });
                } else {//채팅방이 있는 경우
                    chatroom();
                }


            }
        });
        checkChatRoom(false);

    }

    //Push 보내기
    void sendGcm() {
        Gson gson = new Gson();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        NotifyModel notifyModel = new NotifyModel();
        notifyModel.to = receiver.pushToken;
        notifyModel.notification.title = userName;
        notifyModel.notification.text = editText.getText().toString();
        notifyModel.data.title = userName;
        notifyModel.data.text = editText.getText().toString();

        // java를 Json으로 변환
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8"), gson.toJson(notifyModel));

        //헤더
        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .addHeader("Authorization", "key=AIzaSyB2Q82Y_LrM4gK6KmHsYNUxAPv5agwN4rk") //프로젝트 서버키
                .url("https://gcm-http.googleapis.com/gcm/send")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });

    }

    //DB에 채팅내용 저장
    void chatroom() {
        ChatModel.Comment comment = new ChatModel.Comment();
        comment.uid = myUid;
        comment.message = editText.getText().toString();
        //시간 넣기
        comment.timestamp = ServerValue.TIMESTAMP;

        //메세지 저장
        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").push().setValue(comment)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Push 알림
                        sendGcm();
                        //텍스트 초기화
                        editText.setText("");
                    }
                });

        //마지막 코멘트 저장
        Map<String, Object> lastComment = new HashMap<>();
        lastComment.put("lastComment", comment);
        FirebaseDatabase.getInstance().getReference("chatrooms").child(chatRoomUid).updateChildren(lastComment);
    }

    //채팅룸 중복 체크
    void checkChatRoom(final Boolean flag) {
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/" + myUid).equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            ChatModel chatModel = item.getValue(ChatModel.class);
                            if (chatModel.users.containsKey(targetUid)) {

                                //해당 체팅룸에 타겟uid가 있으면 체팅룸 uid 정의
                                chatRoomUid = item.getKey();
                                //연속 클릭 방지
                                button.setEnabled(true);

                                //리사이클러뷰
                                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                                recyclerView.setAdapter(new RecyclerViewAdapter());
                            }
                        }
                        if (flag) {
                            if (chatRoomUid != null) {
                                chatroom();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    //리사이클러뷰
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        List<ChatModel.Comment> comments;

        public RecyclerViewAdapter() {
            comments = new ArrayList<>();

            //DB에서 채팅 가져오기 (뒤로가기에도 읽음이 계속 실행되는걸 방지)
            databaseReference = FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments");
            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //코멘트 정의
                    comments.clear();
                    //읽음 표시
                    Map<String, Object> readUsersMap = new HashMap<>();

                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        String key = item.getKey();
                        ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                        //읽음 태그
                        comment.readUsers.put(myUid, true);
                        readUsersMap.put(key, comment);

                        comments.add(comment);
                    }

                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments")
                            .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //메세지가 갱신
                            notifyDataSetChanged();
                            //스크롤 다운
                            recyclerView.scrollToPosition(comments.size() - 1);
                        }
                    });

                    //마지막 코멘트 읽음표시
                    for (int i = 0; i < 1; i++) {
                        FirebaseDatabase.getInstance().getReference("chatrooms").child(chatRoomUid).child("lastComment")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        ChatModel.LastComment lastCo = dataSnapshot.getValue(ChatModel.LastComment.class);
                                        lastCo.readUsers.put(myUid, true);
                                        Map<String, Object> readUser = new HashMap<>();
                                        readUser.put("lastComment", lastCo);
                                        FirebaseDatabase.getInstance().getReference("chatrooms").child(chatRoomUid).updateChildren(readUser);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                    }


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        }


        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new CustomeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            CustomeViewHolder viewHolder = ((CustomeViewHolder) holder);

            //발신자일 경우
            if (comments.get(position).uid.equals(myUid)) {
                viewHolder.textView_msg.setText(comments.get(position).message);
                viewHolder.textView_msg.setBackgroundResource(R.drawable.right_chat);
                viewHolder.textView_msg.setTextSize(15);
                //프로필 사진, 이름이 필요 없음
                viewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
                //오른쪽 정렬
                viewHolder.linearLayout.setGravity(Gravity.RIGHT);
                //읽음 표시
                setReadCounter(position, viewHolder.textView_readCounter_left);

            } else {//수신자일 경우
                //이미지 불러오기
                Glide.with(holder.itemView.getContext())
                        .load(receiver.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(viewHolder.imageView_profile);

                viewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                viewHolder.textView_msg.setText(comments.get(position).message);
                viewHolder.textView_msg.setBackgroundResource(R.drawable.left_chat);
                viewHolder.textView_msg.setTextSize(15);
                //왼쪽 정렬
                viewHolder.linearLayout.setGravity(Gravity.LEFT);
                //읽음 표시
                setReadCounter(position, viewHolder.textView_readCounter_left);
            }

            //시간 설정
            long unixTime = (long) comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            viewHolder.timestamp.setText(time);


        }

        //읽음 표시
        void setReadCounter(final int position, final TextView textView) {
            FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("users")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Map<String, Boolean> users = (Map<String, Boolean>) dataSnapshot.getValue();

                            int count = users.size() - comments.get(position).readUsers.size();
                            if (count > 0) {
                                textView.setVisibility(View.VISIBLE);
                                textView.setText(String.valueOf(count));
                            } else {
                                textView.setVisibility(View.INVISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class CustomeViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_msg;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout;
            public TextView timestamp;
            public TextView textView_readCounter_left;
            public TextView textView_readCounter_right;

            public CustomeViewHolder(View view) {
                super(view);
                textView_msg = view.findViewById(R.id.msgitem_textView_msg);
                imageView_profile = view.findViewById(R.id.msgitem_imageview_profile);
                linearLayout_destination = view.findViewById(R.id.msgitem_linearLayout_destination);
                linearLayout = view.findViewById(R.id.msgitem_linearLayout);
                timestamp = view.findViewById(R.id.msgitem_timestamp);
                textView_readCounter_left = view.findViewById(R.id.msgitem_readCounter_left);
                textView_readCounter_right = view.findViewById(R.id.msgitem_readCounter_right);
            }
        }
    }

    @Override
    public void onBackPressed() {
        //뒤로가기에도 계속 실행되는걸 방지
        if (chatRoomUid != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
        finish();
    }
}
