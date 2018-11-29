package com.astro.android.astro.fragment;

import android.app.Fragment;
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
import com.astro.android.astro.model.TagsModel;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagsFragment extends Fragment {

    private String tagKey;
    private TextView textView;
    private ImageView back;
    private List<TagsModel> tagsList;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tags, container, false);

        //변수 정의
        tagKey = getArguments().getString("tagKey");
        textView = view.findViewById(R.id.tags_tagKey);
        textView.setText("#" + tagKey);
        back = view.findViewById(R.id.tags_back);

        //DB에서 데이터 가져오기
        fetchData();

        //리사이클러뷰 생성
        recyclerView = view.findViewById(R.id.tags_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        recyclerView.setAdapter(new TagsAdapter());

        //뒤로가기
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //데이터 넣기
                Fragment search = new SearchFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean("category", false);
                bundle.putString("tags_keyword", tagKey);
                search.setArguments(bundle);
                getActivity().getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, search).commit();
            }
        });

        return view;
    }

    //DB에서 데이터 가져오기
    private void fetchData() {
        tagsList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference("tags").child(tagKey).orderByValue().limitToLast(12)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        tagsList.clear();
                        long index = 0;
                        final long tagsSize=dataSnapshot.getChildrenCount();
                        for (DataSnapshot item:dataSnapshot.getChildren()){
                            final TagsModel tagsM = new TagsModel();
                            index++;
                            tagsM.uniqueID = item.getKey();
                            tagsM.timestamp = item.getValue();

                            //이미지 Url 가져오기
                            final long finalIndex = index;
                            FirebaseDatabase.getInstance().getReference("posts").child(tagsM.uniqueID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            tagsM.imageUrl = String.valueOf(dataSnapshot.child("imageUrl").getValue());
                                            tagsList.add(tagsM);

                                            if (finalIndex == tagsSize){
                                                Collections.reverse(tagsList);
                                                recyclerView.getAdapter().notifyDataSetChanged();
                                            }
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

    //어댑터
    private class TagsAdapter extends RecyclerView.Adapter {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tags,parent,false);
            //빈공간 없애기
            int width = parent.getResources().getDisplayMetrics().widthPixels / 3;   //전체 width를 나누기3
            view.setLayoutParams(new LinearLayout.LayoutParams(width, width));

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            CustomViewHolder viewHolder = ((CustomViewHolder)holder);

            Glide.with(holder.itemView.getContext())
                    .load(tagsList.get(position).imageUrl)
                    .into(viewHolder.imageView);

        }

        @Override
        public int getItemCount() {
            return tagsList.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {

            private ImageView imageView;
            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.tags_post);
            }
        }
    }
}
