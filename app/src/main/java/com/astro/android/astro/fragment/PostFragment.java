package com.astro.android.astro.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.astro.android.astro.CommentActivity;
import com.astro.android.astro.EditPostActivity;
import com.astro.android.astro.MainActivity;
import com.astro.android.astro.R;
import com.astro.android.astro.model.PostModel;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostFragment extends Fragment {
    private String uniqueID;
    private ImageView back;
    private ImageView setting;
    private ImageView imageView;
    private ImageView star;
    private ImageView comment;
    private TextView star_count;
    private TextView content;
    private PostModel postModel;
    private String myUid;
    private Boolean checkStar;
    private String starLengthString;
    private CircleImageView profile;
    private TextView username;
    private RelativeLayout post_userinfo;
    private BottomNavigationView bottom;
    private String departure;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_post, container, false);
        //변수 정의
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        uniqueID = getArguments().getString("uniqueID");
        setting = view.findViewById(R.id.post_setting);
        back = view.findViewById(R.id.post_back);
        imageView = view.findViewById(R.id.post_imageview);
        star = view.findViewById(R.id.post_star);
        comment = view.findViewById(R.id.post_comment);
        star_count = view.findViewById(R.id.post_star_count);
        content = view.findViewById(R.id.post_content);
        profile = view.findViewById(R.id.post_profile);
        username = view.findViewById(R.id.post_username);
        post_userinfo = view.findViewById(R.id.post_userinfo);
        bottom = MainActivity.bottom;
        departure = getArguments().getString("departure");

        //데이터 가져오기
        FirebaseDatabase.getInstance().getReference().child("posts").child(uniqueID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        postModel = dataSnapshot.getValue(PostModel.class);

                        //게시자 사진
                        Glide.with(getActivity())
                                .load(postModel.profileImageUrl)
                                .into(profile);
                        //게시자 이름
                        username.setText(postModel.userName);

                        //본인이라면 셋팅 보임
                        if (postModel.writerUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            setting.setVisibility(View.VISIBLE);
                        }

                        //이미지
                        Glide.with(getActivity())
                                .load(postModel.imageUrl)
                                .into(imageView);

                        //좋아요
                        if (postModel.stars.containsKey(myUid) && postModel.stars.containsValue(true)) {
                            checkStar = true;
                            star.setImageResource(R.drawable.icon_full_star);
                        } else {
                            checkStar = false;
                            star.setImageResource(R.drawable.icon_empty_star);
                        }
                        //좋아요 수
                        starLength(postModel.star_count);
                        showStar_count(Integer.parseInt(postModel.star_count));

                        //내용
                        content.setText(postModel.content);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        //게시자 클릭시
        post_userinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment account = new AccountFragment();
                if (postModel.writerUid.equals(myUid)) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("myPage", true);
                    bundle.putString("departure", departure);
                    account.setArguments(bundle);
                    bottom.setSelectedItemId(R.id.action_account);
                    getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).addToBackStack(null).commit();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("myPage", false);
                    bundle.putString("departure", departure);
                    bundle.putString("userUid", "" + postModel.writerUid);
                    account.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).addToBackStack(null).commit();
                }
            }
        });

        //스타 클릭시
        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkStar)//좋아요 된 상태
                {
                    Map<String, Object> starMap = new HashMap<>();
                    starMap.put(myUid, true);
                    FirebaseDatabase.getInstance().getReference().child("posts").child(uniqueID).child("stars/" + myUid).removeValue()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    checkStar = false;
                                    star.setImageResource(R.drawable.icon_empty_star);
                                }
                            });
                } else { //좋아요 아닌 상태
                    Map<String, Object> starM = new HashMap<>();
                    starM.put(myUid, true);
                    //여기 할 차례
                    FirebaseDatabase.getInstance().getReference().child("posts").child(uniqueID).child("stars").updateChildren(starM)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(getActivity(), "Liked", Toast.LENGTH_SHORT).show();
                                    checkStar = true;
                                    star.setImageResource(R.drawable.icon_full_star);
                                }
                            });

                }

                //좋아요 카운터 정의
                FirebaseDatabase.getInstance().getReference().child("posts").child(uniqueID).child("stars")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String count = String.valueOf(dataSnapshot.getChildrenCount());
                                //좋아요 수 재정의
                                starLength(count);
                                star_count.setText(count + starLengthString);

                                Map<String, Object> star_count = new HashMap<>();
                                star_count.put("star_count", count);
                                FirebaseDatabase.getInstance().getReference().child("posts").child(uniqueID).updateChildren(star_count);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        });
        //스타 갯수 계속 읽기
        FirebaseDatabase.getInstance().getReference("posts").child(uniqueID).child("star_count")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            showStar_count(Integer.parseInt(String.valueOf(dataSnapshot.getValue())));
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        //코멘트 눌렸을때
        comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //초기화
                Intent intent = new Intent(getActivity(), CommentActivity.class);
                intent.putExtra("uniqueID", postModel.uniqueID);
                getActivity().startActivity(intent);
            }
        });

        //뒤로가기
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });

        //셋팅 클릭시
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //팝업 메뉴 보이기
                PopupMenu popup = new PopupMenu(getActivity(), view);
                //팝업 메뉴 클릭
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.setting_edit:
                                //게시물 수정
                                Intent intent = new Intent(getActivity(), EditPostActivity.class);
                                intent.putExtra("uniqueID", uniqueID);
                                startActivity(intent);

                                return true;
                            case R.id.setting_delete:
                                //게시물 삭제
                                //다이얼로그 띄우기
                                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                                dialog.setMessage("Will you delete it?")
                                        .setNegativeButton("No", null)
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                //본인 게시물 목록 삭제
                                                FirebaseDatabase.getInstance().getReference("accountPost").child(myUid).child(uniqueID).removeValue();

                                                //게시물 삭제
                                                FirebaseDatabase.getInstance().getReference("posts").child(uniqueID).removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            //태그 삭제
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                String str = postModel.tags;
                                                                String[] split = str.split("#");
                                                                for (int i = 1; i <= split.length - 1; i++) {
                                                                    FirebaseDatabase.getInstance().getReference().child("tags").child(split[i]).child(uniqueID).removeValue();
                                                                }
                                                                Toast.makeText(getActivity(), "Deleted", Toast.LENGTH_SHORT).show();
                                                                getActivity().onBackPressed();
                                                            }
                                                        });
                                            }
                                        })
                                        .show();
                                return true;
                        }
                        return false;
                    }
                });
                popup.inflate(R.menu.popup_menu);
                popup.show();
            }
        });

        return view;
    }

    //스타 보이기
    private void showStar_count(int star_num) {
        if (star_num > 0) {
            star_count.setVisibility(View.VISIBLE);
            star_count.setText(star_num + starLengthString);
        } else {
            star_count.setVisibility(View.GONE);
        }
    }
    //좋아요
    void starLength(String star_count) {
        if (star_count.equals("1") || star_count.equals("0"))
            starLengthString = " star";
        else
            starLengthString = " stars";
    }


}
