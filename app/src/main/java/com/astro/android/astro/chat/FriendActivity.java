package com.astro.android.astro.chat;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Image;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.astro.android.astro.R;
import com.astro.android.astro.fragment.HomeFragment;
import com.astro.android.astro.model.ChatModel;
import com.astro.android.astro.model.UserModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendActivity extends AppCompatActivity {

    private ImageView msg;
    public static Activity friendAct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        friendAct = FriendActivity.this;


        //리사이클러뷰 정의
        RecyclerView view = findViewById(R.id.friend_recyclerview);
        view.setLayoutManager(new LinearLayoutManager(this));
        view.setAdapter(new FriendActivityRecyclerViewAdapter());

        ImageView back = findViewById(R.id.friendactivity_back);

        //뒤로가기
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }

    //리사이클러뷰
    class FriendActivityRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


        private List<UserModel> userModels;
        private List<ChatModel> chatModels;
        private ChatModel newMsg;
        private String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //생성자
        public FriendActivityRecyclerViewAdapter() {
            userModels = new ArrayList<>();
            chatModels = new ArrayList<>();
            //DB에서 친구 목록 가져오기
            FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //친구목록 정의
                    userModels.clear(); //누적 데이터 삭제
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        //본인 제외
                        UserModel userModel = snapshot.getValue(UserModel.class);

                        if (userModel.uid == null || userModel.uid.equals(myUid)) {
                            continue;
                        }


                        userModels.add(userModel);

                    }
                    //새로고침
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            //chat 모델 가져오기
            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/" + myUid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            chatModels.clear();
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                chatModels.add(item.getValue(ChatModel.class));
                            }
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            final CustomViewHolder viewHolder = ((CustomViewHolder) holder);
            //이미지 불러오기
            Glide.with(holder.itemView.getContext())
                    .load(userModels.get(position).profileImageUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(viewHolder.imageView);

            //유저이름 불러오기
            ((CustomViewHolder) holder).textView.setText(userModels.get(position).name);

            //마지막 메세지 가져오기
            if (chatModels.size() != 0) {
                for (int i = 0; i < chatModels.size(); i++) {
                    if (chatModels.get(i).users.containsKey(userModels.get(position).uid)
                            && chatModels.get(i).users.containsKey(myUid)) {

                        viewHolder.notice.setVisibility(View.INVISIBLE);
                        viewHolder.lastmsg.setTypeface(Typeface.DEFAULT);
                        //마지막 메세지
                        viewHolder.lastmsg.setText(chatModels.get(i).lastComment.message);
                        if (!chatModels.get(i).lastComment.readUsers.containsKey(myUid)) {
                            viewHolder.notice.setVisibility(View.VISIBLE);
                            viewHolder.lastmsg.setTypeface(Typeface.DEFAULT_BOLD);
                        }

                    }
                }
            }


            //해당 친구 클릭시 채팅 itemview = 리스트 1개
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), MessageActivity.class);
                    //해당 친구 Uid 전송
                    intent.putExtra("targetUid", userModels.get(position).uid);
                    startActivity(intent);

                }
            });
        }

        @Override
        public int getItemCount() {
            return userModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public ImageView notice;
            public TextView lastmsg;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.frienditem_imageview);
                textView = view.findViewById(R.id.frienditem_textview);
                notice = view.findViewById(R.id.frienditem_notice);
                lastmsg = view.findViewById(R.id.frienditem_lastmsg);
            }
        }
    }

    //뒤로가기
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
