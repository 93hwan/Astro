package com.astro.android.astro.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.widget.RecyclerView;
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
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterHome extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<PostModel> items;
    private String myUid;
    private boolean checkStar;
    private String starLengthString;
    private BottomNavigationView bottom;

    public AdapterHome(Context context, List<PostModel> items, BottomNavigationView bottom) {
        this.context = context;
        this.items = items;
        this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.bottom = bottom;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home, parent, false);

        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        final CustomViewHolder viewHolder = ((CustomViewHolder) holder);

        //데이터 정의
        Glide.with(holder.itemView.getContext())
                .load(items.get(position).profileImageUrl)
                .into(viewHolder.profile);

        viewHolder.username.setText(items.get(position).userName);

        Glide.with(holder.itemView.getContext())
                .load(items.get(position).imageUrl)
                .into(viewHolder.imageView);
        //유저 탭 클릭시
        viewHolder.userinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment account = new AccountFragment();
                if (items.get(position).writerUid.equals(myUid)) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("myPage", true);
                    bundle.putString("departure", "home");
                    account.setArguments(bundle);
                    bottom.setSelectedItemId(R.id.action_account);
                    ((Activity) context).getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).addToBackStack(null).commit();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("myPage", false);
                    bundle.putString("departure", "home");
                    bundle.putString("userUid", "" + items.get(position).writerUid);
                    account.setArguments(bundle);
                    ((Activity) context).getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).addToBackStack(null).commit();
                }
            }
        });

        //셋팅
        if (items.get(position).writerUid.equals(myUid)) {
            viewHolder.setting.setVisibility(View.VISIBLE);
        } else {
            viewHolder.setting.setVisibility(View.INVISIBLE);
        }

        //스타 내가 눌렸는지 확인
        final String postUid = items.get(position).uniqueID;
        FirebaseDatabase.getInstance().getReference().child("posts").child(postUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        PostModel postcheck = dataSnapshot.getValue(PostModel.class);

                        if (postcheck.stars.containsKey(myUid) && postcheck.stars.containsValue(true)) {
                            checkStar = true;
                            viewHolder.star.setImageResource(R.drawable.icon_full_star);
                        } else {
                            checkStar = false;
                            viewHolder.star.setImageResource(R.drawable.icon_empty_star);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        //스타 눌렸을때
        viewHolder.star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkStar) {//제거
                    Map<String, Object> star = new HashMap<>();
                    star.put(myUid, true);
                    FirebaseDatabase.getInstance().getReference().child("posts").child(items.get(position).uniqueID).child("stars/" + myUid).removeValue()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    checkStar = false;
                                    viewHolder.star.setImageResource(R.drawable.icon_empty_star);
                                }
                            });
                } else {//좋아요
                    Map<String, Object> star = new HashMap<>();
                    star.put(myUid, true);
                    //여기 할 차례
                    FirebaseDatabase.getInstance().getReference().child("posts").child(items.get(position).uniqueID).child("stars").updateChildren(star)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(context, "Liked", Toast.LENGTH_SHORT).show();
                                    checkStar = true;
                                    viewHolder.star.setImageResource(R.drawable.icon_full_star);
                                }
                            });
                }

                //좋아요 카운터 정의
                FirebaseDatabase.getInstance().getReference().child("posts").child(items.get(position).uniqueID).child("stars")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String count = String.valueOf(dataSnapshot.getChildrenCount());
                                //좋아요 수 재정의
                                starLength(count);
                                viewHolder.star_count.setText(count + starLengthString);

                                Map<String, Object> star_count = new HashMap<>();
                                star_count.put("star_count", count);
                                FirebaseDatabase.getInstance().getReference().child("posts").child(items.get(position).uniqueID).updateChildren(star_count);

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

            }
        });

        //스타 갯수 계속 읽기
        FirebaseDatabase.getInstance().getReference("posts").child(items.get(position).uniqueID).child("star_count")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null){
                            showStar_count(viewHolder, Integer.parseInt(String.valueOf(dataSnapshot.getValue())));
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        //코멘트 눌렸을때
        viewHolder.comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //초기화
                Intent intent = new Intent(context, CommentActivity.class);
                intent.putExtra("uniqueID", items.get(position).uniqueID);
                context.startActivity(intent);
            }
        });

        //좋아요 수
        starLength(items.get(position).star_count);
        showStar_count(viewHolder, Integer.parseInt(items.get(position).star_count));

        viewHolder.content.setText(items.get(position).content);

        //셋팅 클릭시
        viewHolder.setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //팝업 메뉴 보이기
                PopupMenu popup = new PopupMenu(context, view);
                //팝업 메뉴 클릭
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.setting_edit:
                                //게시물 수정
                                Intent intent = new Intent(context, EditPostActivity.class);
                                intent.putExtra("uniqueID", items.get(position).uniqueID);
                                context.startActivity(intent);

                                return true;
                            case R.id.setting_delete:
                                //게시물 삭제
                                //다이얼로그 띄우기
                                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                                dialog.setMessage("Will you delete it?")
                                        .setNegativeButton("No", null)
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                //본인 게시물 목록 삭제
                                                FirebaseDatabase.getInstance().getReference("accountPost").child(myUid).child(items.get(position).uniqueID).removeValue();
                                                //게시물 삭제
                                                FirebaseDatabase.getInstance().getReference("posts").child(items.get(position).uniqueID).removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            //태그 삭제
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                String str = items.get(position).tags;
                                                                String[] split = str.split("#");
                                                                for (int i = 1; i <= split.length - 1; i++) {
                                                                    FirebaseDatabase.getInstance().getReference().child("tags").child(split[i]).child(items.get(position).uniqueID).removeValue();
                                                                }
                                                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
                                                                ((Activity)context).getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new HomeFragment()).commit();
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
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    //스타 보이기
    private void showStar_count(CustomViewHolder viewHolder, int star_num) {
        if (star_num > 0) {
            viewHolder.star_count.setVisibility(View.VISIBLE);
            viewHolder.star_count.setText(star_num + starLengthString);
        } else {
            viewHolder.star_count.setVisibility(View.GONE);
        }
    }

    private class CustomViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout userinfo;
        public CircleImageView profile;
        public TextView username;
        public ImageView imageView;
        public ImageView star;
        public ImageView comment;
        public TextView star_count;
        public TextView content;
        public ImageView setting;

        public CustomViewHolder(View view) {
            super(view);
            userinfo = view.findViewById(R.id.homeitem_userinfo);
            profile = view.findViewById(R.id.homeitem_profile);
            username = view.findViewById(R.id.homeitem_username);
            imageView = view.findViewById(R.id.homeitem_imageview);
            star = view.findViewById(R.id.homeitem_star);
            comment = view.findViewById(R.id.homeitem_comment);
            star_count = view.findViewById(R.id.homeitem_star_count);
            content = view.findViewById(R.id.homeitem_content);
            setting = view.findViewById(R.id.homeitem_setting);

        }
    }

    //좋아요
    void starLength(String star_count) {
        if (star_count.equals("1") || star_count.equals("0"))
            starLengthString = " star";
        else
            starLengthString = " stars";
    }

    void refresh(List<PostModel> items) {
        //재정의
        this.items = items;
        notifyDataSetChanged();
    }
}
