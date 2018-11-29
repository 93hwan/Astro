package com.astro.android.astro.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.astro.android.astro.R;
import com.astro.android.astro.chat.FriendActivity;
import com.astro.android.astro.model.ChatModel;
import com.astro.android.astro.model.PostModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout swipe;
    private RecyclerView recyclerView;
    private AdapterHome adapter;
    private LinearLayoutManager manager;
    private List<PostModel> items;
    private List<PostModel> pastItems;
    int lastVisibleItemPosition, totalItemCount;
    private ProgressBar progressBar;
    private ImageView msg;
    private BottomNavigationView bottom;
    private String myUid;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        //초기화
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        bottom = getActivity().findViewById(R.id.mainactivity_bottom_navi);
        //Pull to refresh
        swipe = view.findViewById(R.id.homefragment_swipe);

        //메세지 클릭시
        msg = view.findViewById(R.id.homefragment_msg);
        msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), FriendActivity.class));
            }
        });
        recyclerView = view.findViewById(R.id.homefragment_recyclerview);

        //프로그래스바
        progressBar = view.findViewById(R.id.homefragment_progressbar);

        //뷰설정
        manager = new LinearLayoutManager(inflater.getContext());
        recyclerView.setLayoutManager(manager);

        //어댑터
        adapter = new AdapterHome(getActivity(), createItemList(), bottom);
        recyclerView.setAdapter(adapter);

        //Pull to refresh 새로고침
        swipe.setOnRefreshListener(this);

        //메세지 알림표시
        msgNotification();

        //Endless RecylcerView
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                lastVisibleItemPosition = manager.findLastCompletelyVisibleItemPosition(); //마지막 보이는 아이템 위치

                if (lastVisibleItemPosition == items.size() - 1 && items.size() % 5 == 0
                        && items.size() != 0) { //마지막 조건은 새로고침시 실행이 안되도록
                    progressBar.setVisibility(View.VISIBLE);
                    //지난 포스트 가져오기
                    fetchData();
                }
            }
        });


        return view;
    }

    //메세지 알림표시
    public void msgNotification() {
        final List<ChatModel> chatModels = new ArrayList<>();

        //chat 모델 가져오기
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/" + myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int checking = 0;
                        chatModels.clear();
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            chatModels.add(item.getValue(ChatModel.class));
                        }
                        for (ChatModel item : chatModels) {
                            if (item.users.containsKey(myUid) && item.lastComment.uid != null) {
                                if (!item.lastComment.uid.contains(myUid) && !item.lastComment.readUsers.containsKey(myUid)) {
                                    checking++;
                                }
                                if (checking > 0) {
                                    msg.setImageResource(R.drawable.icon_newmsg);
                                } else {
                                    msg.setImageResource(R.drawable.icon_msg);
                                }
                            }

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    //DB에서 post 리스트 가져오기
    public List<PostModel> createItemList() {
        items = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference().child("posts").orderByChild("timestamp").limitToLast(5)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        items.clear();
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            PostModel post = item.getValue(PostModel.class);
                            items.add(post);
                        }
                        //내림차순으로 바꾸기
                        Collections.reverse(items);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        return items;
    }

    //지난 포스트 가져오기
    public void fetchData() {
        pastItems = new ArrayList<>();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final Long lastViewTime = (Long) items.get(items.size() - 1).timestamp;
                FirebaseDatabase.getInstance().getReference().child("posts").orderByChild("timestamp").endAt(lastViewTime).limitToLast(6)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                pastItems.clear();
                                for (DataSnapshot item : dataSnapshot.getChildren()) {
                                    PostModel post = item.getValue(PostModel.class);
                                    if (post.timestamp.equals(lastViewTime)) {
                                        continue;
                                    }
                                    pastItems.add(post);
                                }
                                Collections.reverse(pastItems);
                                for (PostModel item : pastItems) {
                                    items.add(item);
                                }
                                progressBar.setVisibility(View.INVISIBLE);
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

            }
        }, 3000);

    }

    //Pull to refresh 새로고침
    @Override
    public void onRefresh() {
        Toast.makeText(getActivity(), "Refresh", Toast.LENGTH_SHORT).show();
        adapter.refresh(createItemList());
        swipe.setRefreshing(false);
    }

}
