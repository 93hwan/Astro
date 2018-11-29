package com.astro.android.astro.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.astro.android.astro.R;
import com.astro.android.astro.model.AccountPostModel;
import com.bumptech.glide.Glide;

import java.util.List;

public class AdapterAccount extends RecyclerView.Adapter {
    private Context context;
    private List<AccountPostModel> items;

    public AdapterAccount(Context context, List<AccountPostModel> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accountpost, parent, false);

        //빈공간 없애기
        int width = parent.getResources().getDisplayMetrics().widthPixels/3;   //전체 width를 나누기3
        view.setLayoutParams(new LinearLayout.LayoutParams(width,width));

        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        CustomViewHolder viewHolder = ((CustomViewHolder) holder);

        Glide.with(holder.itemView.getContext())
                .load(items.get(position).imageUrl)
                .into(viewHolder.imageView);


        //클릭시 post로 보내기
        viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString("uniqueID",items.get(position).uniqueID);
                Fragment post = new PostFragment();
                post.setArguments(bundle);
                ((Activity)context).getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout,post).addToBackStack(null).commit();

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
            imageView = view.findViewById(R.id.accountpostitem_post);
        }
    }

    void refresh(List<AccountPostModel> items){
        //재정의
        this.items = items;
        notifyDataSetChanged();
    }
}
