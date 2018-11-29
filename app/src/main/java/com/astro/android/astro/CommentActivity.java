package com.astro.android.astro;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.astro.android.astro.fragment.AccountFragment;
import com.astro.android.astro.model.PostModel;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentActivity extends AppCompatActivity {

    private ImageView back;
    private CircleImageView writerProfile;
    private TextView writerName;
    private TextView writerContent;
    private TextView writertimestamp;
    private String uniqueID;
    private PostModel postModel;
    private EditText comment_text;
    private Button comment_send;
    private String myUid;
    private RecyclerView recyclerView;
    private List<PostModel.PostComment> commentList;
    private BottomNavigationView bottom;

    //시간
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd EEE HH:mm aa");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        //변수 정의
        bottom = MainActivity.bottom;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        uniqueID = getIntent().getStringExtra("uniqueID");
        writerProfile = findViewById(R.id.comment_writerprofile);
        writerName = findViewById(R.id.comment_writername);
        writerContent = findViewById(R.id.comment_writercomment);
        back = findViewById(R.id.comment_back);
        comment_text = findViewById(R.id.comment_edittext);
        comment_send = findViewById(R.id.comment_btn_post);
        writertimestamp = findViewById(R.id.comment_timestamp);

        //bottom 숨기기
        bottom.setVisibility(View.GONE);

        //뒤로가기
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        //작성자 클릭시
        writerProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment account = new AccountFragment();
                if (postModel.writerUid.equals(myUid)) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("myPage", true);
                    account.setArguments(bundle);
                    bottom.setSelectedItemId(R.id.action_account);
                    getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).commit();

                } else {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("myPage", false);
                    bundle.putString("userUid",postModel.writerUid);
                    account.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).addToBackStack(null).commit();

                }
            }
        });

        //리사이클러뷰
        recyclerView = findViewById(R.id.comment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(new PostCommentAdapter());

        //작성자 데이터 가져오기
        FirebaseDatabase.getInstance().getReference().child("posts").child(uniqueID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        postModel = dataSnapshot.getValue(PostModel.class);
                        Glide.with(getApplicationContext())
                                .load(postModel.profileImageUrl)
                                .into(writerProfile);

                        writerName.setText(postModel.userName);
                        writerContent.setText(postModel.content);
                        //시간 설정
                        long unixTime = (long) postModel.timestamp;
                        Date date = new Date(unixTime);
                        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                        String time = simpleDateFormat.format(date);
                        writertimestamp.setText(time);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        //댓글 저장
        comment_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PostModel.PostComment postComment = new PostModel.PostComment();
                postComment.uid = myUid;
                postComment.comment = comment_text.getText().toString().trim();
                postComment.username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                postComment.timestamp = ServerValue.TIMESTAMP;
                postComment.profileImageUrl = String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl());
                String commentID = UUID.randomUUID().toString();
                FirebaseDatabase.getInstance().getReference("posts").child(uniqueID).child("postComments").child(commentID).setValue(postComment)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //초기화
                                recyclerView.setAdapter(new PostCommentAdapter());
                                comment_text.setText("");
                            }
                        });

            }
        });
    }
    //바텀 보이게 하기
    @Override
    public void onPause() {
        super.onPause();
        bottom.setVisibility(View.VISIBLE);
    }

    private class PostCommentAdapter extends RecyclerView.Adapter {


        public PostCommentAdapter() {
            //포스트 코멘트 가져오기
            commentList = new ArrayList<>();
            FirebaseDatabase.getInstance().getReference("posts").child(uniqueID).child("postComments").orderByChild("timestamp")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            commentList.clear();
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                commentList.add(item.getValue(PostModel.PostComment.class));
                            }
                            notifyDataSetChanged();
                            //스크롤 다운
                            recyclerView.scrollToPosition(commentList.size() - 1);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);

            return new PostCommentAdapter.CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            PostCommentAdapter.CustomViewHolder viewHolder = ((PostCommentAdapter.CustomViewHolder)holder);
            Glide.with(holder.itemView.getContext())
                    .load(commentList.get(position).profileImageUrl)
                    .into(viewHolder.profile);
            //유저 탭 클릭시
            viewHolder.profile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Fragment account = new AccountFragment();
                    if (commentList.get(position).uid.equals(myUid)) {
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("myPage", true);
                        account.setArguments(bundle);
                        bottom.setSelectedItemId(R.id.action_account);
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).commit();

                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("myPage", false);
                        bundle.putString("userUid", "" + commentList.get(position).uid);
                        account.setArguments(bundle);
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).addToBackStack(null).commit();

                    }
                }
            });

            viewHolder.username.setText(commentList.get(position).username);
            viewHolder.comment.setText(commentList.get(position).comment);

            //시간 설정
            long unixTime = (long) commentList.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            viewHolder.timestamp.setText(time);
        }

        @Override
        public int getItemCount() {
            return commentList.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private CircleImageView profile;
            private TextView username;
            private TextView comment;
            private TextView timestamp;

            public CustomViewHolder(View view) {
                super(view);
                profile = view.findViewById(R.id.itemcomment_profile);
                username = view.findViewById(R.id.itemcomment_username);
                comment = view.findViewById(R.id.itemcomment_comment);
                timestamp = view.findViewById(R.id.itemcomment_timestamp);
            }
        }
    }
}
