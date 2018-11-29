package com.astro.android.astro.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.astro.android.astro.EditAccountActivity;
import com.astro.android.astro.MainActivity;
import com.astro.android.astro.R;
import com.astro.android.astro.SettingActivity;
import com.astro.android.astro.model.AccountPostModel;
import com.astro.android.astro.model.UserModel;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, MainActivity.OnBackPressedListener {

    private ImageView back;
    private ImageView setting;
    private CircleImageView profile;
    private TextView post_count;
    private TextView friend_count;
    private Button editButton;
    private TextView profile_msg;
    private UserModel userModel;
    private String post_child_num;
    private boolean myPage;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipe;
    private List<AccountPostModel> items;
    private List<AccountPostModel> pastItems;
    private AdapterAccount adapter;
    private GridLayoutManager manager;
    private ProgressBar progressBar;
    private TextView userName;
    private String userUid;
    private BottomNavigationView bottom;
    private String departure;
    private TextView bio;


    int lastVisibleItemPosition, totalItemCount;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);


        //변수 정의
        back = view.findViewById(R.id.accountfragment_back);
        myPage = getArguments().getBoolean("myPage");
        departure = getArguments().getString("departure");
        setting = view.findViewById(R.id.accountfragment_setting);
        profile = view.findViewById(R.id.accountfragment_profile);
        post_count = view.findViewById(R.id.accountfragment_post_count);
        friend_count = view.findViewById(R.id.accountfragment_friend_count);
        editButton = view.findViewById(R.id.accountfragment_editbutton);
        profile_msg = view.findViewById(R.id.accountfragment_bio);
        userName = view.findViewById(R.id.accountfragment_username);
        bottom = getActivity().findViewById(R.id.mainactivity_bottom_navi);
        bio = view.findViewById(R.id.accountfragment_bio);

        //유저 분류
        if (myPage) { //본인 일때
            userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            editButton.setVisibility(View.VISIBLE);
            back.setVisibility(View.GONE);

            //프로필 수정
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent intent = new Intent(getActivity(), EditAccountActivity.class);
                    intent.putExtra("userUid",userUid);
                    startActivity(intent);

                }
            });

        } else {//본인 아닐때
            back.setVisibility(View.VISIBLE);
            setting.setVisibility(View.INVISIBLE);
            userUid = getArguments().getString("userUid");
        }
        //유저 데이터 정의
        userSetting();

        //리사이클러뷰, 뷰설정
        recyclerView = view.findViewById(R.id.accountfragment_recylcerview);
        manager = new GridLayoutManager(getActivity(), 3);
        recyclerView.setLayoutManager(manager);

        //어댑터 설정
        adapter = new AdapterAccount(getActivity(), createItemList());
        recyclerView.setAdapter(adapter);

        //프로그래스바
        progressBar = view.findViewById(R.id.accountfragment_progressbar);

        //새로고침
        swipe = view.findViewById(R.id.accountfragment_swipe);
        swipe.setOnRefreshListener(this);


        //Endless RecyclerView
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                lastVisibleItemPosition = manager.findLastCompletelyVisibleItemPosition(); //마지막 보이는 아이템 위치

                if (lastVisibleItemPosition == items.size() - 1 && items.size() % 12 == 0
                        && items.size() != 0) {// 마지막 조건은 새로고침시 실행이 안되도록
                    progressBar.setVisibility(View.VISIBLE);
                    //지난 포스트 가져오기
                    fetData();
                }
            }
        });

        //setting으로 이동
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SettingActivity.class);
                startActivity(intent);
            }
        });

        //뒤로가기
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });

        return view;
    }

    //뒤로가기 감지
    @Override
    public void onBack() {

        //출발지 나누기
        switch (departure) {
            case "home":
                bottom.setSelectedItemId(R.id.action_home);
                getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new HomeFragment()).commit();
                break;
            case "chat":
                bottom.setSelectedItemId(R.id.action_home);
                getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new HomeFragment()).commit();
                break;
            case "search":
                bottom.setSelectedItemId(R.id.action_home);
                getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new SearchMainFragment()).commit();
                break;
            default:
                ((MainActivity) getActivity()).setOnBackPressedListener(null);
                getActivity().onBackPressed();

        }

    }

    //뒤로가기 감지
    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).setOnBackPressedListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((MainActivity) getActivity()).setOnBackPressedListener(null);
    }

    //유저 데이터 정의
    public void userSetting() {
        //해당 사용자 페이지 usermodel 프로필사진  가져오기
        userModel = new UserModel();
        FirebaseDatabase.getInstance().getReference().child("users").orderByChild("uid").equalTo(userUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            userModel = item.getValue(UserModel.class);
                            //데이터 정의
                            Glide.with(getActivity())
                                    .load(userModel.profileImageUrl)
                                    .into(profile);
                            userName.setText(userModel.name);
                            profile_msg.setText(userModel.profilemsg);
                            bio.setText(userModel.bio);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        //해당 유저 포스트 수 가져오기
        FirebaseDatabase.getInstance().getReference().child("posts").orderByChild("writerUid").equalTo(userUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        post_child_num = String.valueOf(dataSnapshot.getChildrenCount());
                        //데이터 정의
                        post_count.setText(post_child_num);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

    }

    //DB에서 데이터 가져오기
    public List<AccountPostModel> createItemList() {
        items = new ArrayList<>();

        FirebaseDatabase.getInstance().getReference("accountPost").child(userUid).orderByChild("timestamp").limitToLast(12)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        items.clear();
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            AccountPostModel post = item.getValue(AccountPostModel.class);
                            items.add(post);
                        }
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
    public void  fetData() {
        pastItems = new ArrayList<>();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final Long lastViewTime = (Long) items.get(items.size() - 1).timestamp;
                FirebaseDatabase.getInstance().getReference("accountPost").child(userUid).orderByChild("timestamp").endAt(lastViewTime).limitToLast(13)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                pastItems.clear();
                                for (DataSnapshot item : dataSnapshot.getChildren()) {
                                    AccountPostModel post = item.getValue(AccountPostModel.class);
                                    if (post.timestamp.equals(lastViewTime)) {
                                        continue;
                                    }
                                    pastItems.add(post);
                                }
                                Collections.reverse(pastItems);
                                for (AccountPostModel item : pastItems) {
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

    //새로고침
    @Override
    public void onRefresh() {
        Toast.makeText(getActivity(), "Refresh", Toast.LENGTH_SHORT).show();
        adapter.refresh(createItemList());
        swipe.setRefreshing(false);
    }

}
