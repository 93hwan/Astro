package com.astro.android.astro;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.astro.android.astro.model.PostModel;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditPostActivity extends AppCompatActivity {
    private ImageView imageView;
    private EditText tag;
    private EditText content;
    private Button edit;
    private String uniqueID;
    private PostModel postModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        //변수 정의
        uniqueID = getIntent().getStringExtra("uniqueID");
        imageView = findViewById(R.id.edit_post_imageview);
        tag = findViewById(R.id.edit_post_tag);
        content = findViewById(R.id.edit_post_content);
        edit = findViewById(R.id.edit_post_edit);

        //수정할 게시물 가져오기
        FirebaseDatabase.getInstance().getReference("posts").child(uniqueID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        postModel = dataSnapshot.getValue(PostModel.class);

                        //수정할 게시물 데이터 초기화
                        Glide.with(getApplicationContext())
                                .load(postModel.imageUrl)
                                .into(imageView);
                        tag.setText(postModel.tags);
                        content.setText(postModel.content);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        //수정하기
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String tagline = tag.getText().toString().replaceAll(" ", "");
                //태그 개수 찾기
                int count = checkTag();
                if (tagline.length() > 0 && tag.getText().toString().indexOf('#') == -1) {//태그가 없을때
                    Toast.makeText(getApplicationContext(), "Input \"#\" in front of your tag ", Toast.LENGTH_SHORT).show();
                    return;
                } else if (tagline.contains("#") && tagline.replaceAll("#", "").length() == 0) {//태그는 있고 키워드가 없을때
                    Toast.makeText(getApplicationContext(), "Type your keyword ", Toast.LENGTH_SHORT).show();
                    return;
                } else if (count > 5) {//태그가 5개 이상 일때
                    Toast.makeText(getApplicationContext(), "Input only 5 tags", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    //기존 태그 삭제
                    String str = postModel.tags;
                    String[] split = str.split("#");
                    for (int i = 1; i <= split.length - 1; i++) {
                        FirebaseDatabase.getInstance().getReference().child("tags").child(split[i]).child(uniqueID).removeValue();
                    }
                    //수정 데이터 저장
                    postModel.tags=tagline;
                    postModel.content = content.getText().toString().trim();

                    //태그 DB에 저장
                    saveTag(postModel.uniqueID, postModel.timestamp);

                    //업데이트용 map 생성
                    Map<String,Object> editedPost = new HashMap<>();
                    editedPost.put(uniqueID,postModel);

                    //DB에 저장
                    FirebaseDatabase.getInstance().getReference("posts").updateChildren(editedPost)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(getApplicationContext(),"Edited",Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                }
            }
        });

    }


    //태그 개수 찾기
    public int checkTag() {
        String str = tag.getText().toString();
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '#') {
                count++;
            }
        }
        return count;
    }

    //태그 저장
    public void saveTag(String uniqueID, Object timestamp) {

        String str = tag.getText().toString().replaceAll(" ", "");
        String[] split = str.split("#");
        for (int i = 1; i <= split.length - 1; i++) {
            FirebaseDatabase.getInstance().getReference().child("tags").child(split[i]).child(uniqueID).setValue(timestamp);
        }
    }
}
