package com.astro.android.astro.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.astro.android.astro.R;
import com.astro.android.astro.model.TagsModel;
import com.astro.android.astro.model.UserModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private boolean category;
    private EditText search;
    private TextView people;
    private TextView tags;
    private RecyclerView recyclerView;
    private int item_view;
    private String keyword;
    private List<UserModel> users;
    private List<TagsModel.TagsSearch> taglist;
    private ImageView back;
    private String myUid;
    private String fromTags;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        //키보드 올려주기
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        //변수 정의
        category = getArguments().getBoolean("category");
        search = view.findViewById(R.id.search_keyword);
        people = view.findViewById(R.id.searchfragment_people);
        tags = view.findViewById(R.id.searchfragment_tags);
        back = view.findViewById(R.id.search_back);
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //tags 페이지에서 돌아온다면
        fromTags = getArguments().getString("tags_keyword");
        if (fromTags != null)
            search.setText(fromTags);

        //리사이클러뷰
        recyclerView = view.findViewById(R.id.searchfragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //카테고리 처리
        people.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//people 카테고리
                //키보드 내리기
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                tags.setTypeface(null, Typeface.NORMAL);
                people.setTypeface(null, Typeface.BOLD);
                category = true;

                //검색
                if (search.getText().toString().replaceAll(" ", "").length() > 0) {
                    searchBy();
                }
            }
        });
        tags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//tags 카테고리
                //키보드 내리기
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                people.setTypeface(null, Typeface.NORMAL);
                tags.setTypeface(null, Typeface.BOLD);
                category = false;

                //검색 && 어댑터 처리
                if (search.getText().toString().replaceAll(" ", "").length() > 0) {
                    searchBy();
                }

            }
        });

        //엔터 처리
        search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                    //키보드 내리기
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    searchBy();
                    return true;
                }
                return false;
            }
        });

        //뒤로가기
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //키보드 내리기
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                getActivity().onBackPressed();
            }
        });

        return view;
    }

    //검색
    public void searchBy() {
        if (category) {//people 카테고리
            item_view = R.layout.item_search_people;
            recyclerView.setAdapter(new SearchRecyclerViewAdapter());
            keyword = search.getText().toString().replaceAll(" ", "");

            users = new ArrayList<>();
            FirebaseDatabase.getInstance().getReference().child("users").orderByChild("name").startAt(keyword).endAt(keyword + "\uf8ff")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            users.clear();
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                UserModel user = item.getValue(UserModel.class);
                                users.add(user);
                            }
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

        } else {//tags 카테고리
            item_view = R.layout.item_search_tags;
            recyclerView.setAdapter(new SearchRecyclerViewAdapter());
            keyword = search.getText().toString().replaceAll(" ", "");

            taglist = new ArrayList<>();
            FirebaseDatabase.getInstance().getReference("tags").orderByKey().startAt(keyword).endAt(keyword + "\uf8ff")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                TagsModel.TagsSearch tagsM = new TagsModel.TagsSearch();
                                tagsM.tagKey = item.getKey();
                                tagsM.tagCount = String.valueOf(item.getChildrenCount());

                                taglist.add(tagsM);
                            }
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }
    }

    //어댑터
    private class SearchRecyclerViewAdapter extends RecyclerView.Adapter {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(item_view, parent, false);

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            CustomViewHolder viewHolder = ((CustomViewHolder) holder);

            if (category) {//people 카테고리
                Glide.with(holder.itemView.getContext())
                        .load(users.get(position).profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(viewHolder.ppl_imageview);

                viewHolder.ppl_textview.setText(users.get(position).name);

                //유저 탭 클릭시
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Fragment account = new AccountFragment();
                        if (users.get(position).uid.equals(myUid)) {
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("myPage", true);
                            bundle.putString("departure", "search");
                            account.setArguments(bundle);
                            getActivity().getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).commit();
                        } else {
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("myPage", false);
                            bundle.putString("departure", "search");
                            bundle.putString("userUid", "" + users.get(position).uid);
                            account.setArguments(bundle);
                            getActivity().getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).commit();
                        }
                    }
                });

            } else {// tags 카테고리
                viewHolder.tag_textview.setText("#" + taglist.get(position).tagKey);
                String post_count;
                if (Integer.parseInt(taglist.get(position).tagCount) > 1) {
                    post_count = " posts";
                }else {
                    post_count = " post";
                }
                viewHolder.tag_tagCount.setText(taglist.get(position).tagCount + post_count);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Fragment tagsFragment = new TagsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("tagKey", taglist.get(position).tagKey);
                        tagsFragment.setArguments(bundle);
                        getActivity().getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, tagsFragment).commit();
                    }
                });
            }

        }

        @Override
        public int getItemCount() {
            if (category) {
                return users.size();
            } else {
                return taglist.size();
            }
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ImageView ppl_imageview;
            private TextView ppl_textview;
            private TextView tag_textview;
            private TextView tag_tagCount;

            public CustomViewHolder(View view) {
                super(view);
                if (category) {
                    ppl_imageview = view.findViewById(R.id.searchpeople_item_imageview);
                    ppl_textview = view.findViewById(R.id.searchpeople_item_textview);
                } else {
                    tag_textview = view.findViewById(R.id.searchtags_item_textview);
                    tag_tagCount = view.findViewById(R.id.searchtags_item_tagcount);
                }

            }
        }
    }
}
