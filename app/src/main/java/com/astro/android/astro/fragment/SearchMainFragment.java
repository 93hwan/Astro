package com.astro.android.astro.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.astro.android.astro.R;
import com.astro.android.astro.model.PostModel;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchMainFragment extends Fragment {
    private TextView textView;
    private PostModel post;
    private PostModel firstPost;
    private PostModel secondPost;
    private RecyclerView recyclerView;
    private List<PostModel> items;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_main, container, false);

        textView = view.findViewById(R.id.search_keyword);

        //리사이클러 뷰
        recyclerView = view.findViewById(R.id.searchfragment_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        recyclerView.setAdapter(new RecyclerViewAdapter(getActivity(),fetchData()));


        //searchFragment 이동
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //데이터 넣기
                Fragment search = new SearchFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean("category",true);
                search.setArguments(bundle);
                getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout,search).addToBackStack(null).commit();
            }
        });

        return view;
    }

    //데이터 불러오기 (하루 동안 스타가 제일 많은 포스트)
    public List<PostModel> fetchData() {
        items = new ArrayList<>();
        post = new PostModel();
        firstPost = new PostModel();
        secondPost = new PostModel();
        long start = new Date().getTime() - (24 * 60 * 60 * 1000);
        long end = new Date().getTime();
        FirebaseDatabase.getInstance().getReference().child("posts").orderByChild("timestamp").startAt(start).endAt(end)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        items.clear();
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            PostModel postM = item.getValue(PostModel.class);
                            items.add(postM);
                        }
                        //스타 버블
                        for (int i = items.size(); i > 0; i--) {
                            for (int j = 0; j < i - 1; j++) {
                                firstPost = items.get(j);
                                secondPost = items.get(j + 1);
                                if (Long.parseLong(firstPost.star_count)
                                        < Long.parseLong(secondPost.star_count)) {
                                    post = firstPost;
                                    items.remove(j);
                                    items.add(j,secondPost);
                                    items.remove(j+1);
                                    items.add(j+1,post);
                                }
                            }
                        }
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        return items;
    }

    //어댑터
    private class RecyclerViewAdapter extends RecyclerView.Adapter {
        private Context context;
        private List<PostModel> items;

        public RecyclerViewAdapter(Context context,List<PostModel> post) {
            this.context = context;
            this.items = post;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_main, parent, false);
            //빈공간 없애기
            int width = parent.getResources().getDisplayMetrics().widthPixels / 3;   //전체 width를 나누기3
            view.setLayoutParams(new LinearLayout.LayoutParams(width, width));

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

            CustomViewHolder viewHolder=((CustomViewHolder)holder);

            Glide.with(holder.itemView.getContext())
                    .load(items.get(position).imageUrl)
                    .into(viewHolder.imageView);
            //post로 이동
            viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle bundle = new Bundle();
                    bundle.putString("uniqueID",items.get(position).uniqueID);
                    bundle.putString("departure","search");
                    Fragment post = new PostFragment();
                    post.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout,post).addToBackStack(null).commit();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.searchitem_main_post);
            }
        }
    }
}