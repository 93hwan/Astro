package com.astro.android.astro.fragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.internal.BottomNavigationMenu;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.astro.android.astro.R;
import com.astro.android.astro.model.AccountPostModel;
import com.astro.android.astro.model.PostModel;
import com.astro.android.astro.model.TagsModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class UploadFragment extends Fragment {

    private static final int PICK_FROM_ALBUM = 11;
    private ImageView imageView;
    private EditText tag;
    private EditText content;
    private Button upload;
    private Uri imageUri;
    private ProgressDialog dialog;
    private String uid;
    private BottomNavigationView bottom;
    private PostModel postModel = new PostModel();
    private AccountPostModel aPost = new AccountPostModel();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);
        bottom = getActivity().findViewById(R.id.mainactivity_bottom_navi);

        //변수 정의
        imageView = view.findViewById(R.id.uploadfragment_imageview);
        tag = view.findViewById(R.id.uploadfragment_tag);
        content = view.findViewById(R.id.uploadfragment_content);
        upload = view.findViewById(R.id.uploadfragment_upload);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();


        //사진 올리기
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_ALBUM);
            }
        });
        //프로그래스바
        dialog = new ProgressDialog(getActivity());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);


        //업로드
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final String tagline = tag.getText().toString().replaceAll(" ", "");
                //태그 개수 찾기
                int count = checkTag();
                if (imageUri == null) { //사진 없을때
                    Toast.makeText(getActivity(), "Add your picture!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (tagline.length() > 0 && tag.getText().toString().indexOf('#') == -1) {//태그가 없을때
                    Toast.makeText(getActivity(), "Input \"#\" in front of your tag ", Toast.LENGTH_SHORT).show();
                    return;
                } else if (tagline.contains("#") && tagline.replaceAll("#", "").length() == 0) {//태그는 있고 키워드가 없을때
                    Toast.makeText(getActivity(), "Type your keyword ", Toast.LENGTH_SHORT).show();
                    return;
                } else if (count > 5) {//태그가 5개 이상 일때
                    Toast.makeText(getActivity(), "Input only 5 tags", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    //다중 클릭 방지, progress 바
                    upload.setEnabled(false);
                    dialog.show();
                    dialog.setContentView(R.layout.item_progress);
                    //UUID 생성 for storage
                    final String uniqueID = UUID.randomUUID().toString();

                    //저장소에 포스트 사진 저장
                    FirebaseStorage.getInstance().getReference().child("postImages").child(uid).child(uniqueID).putFile(imageUri)
                            .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    //정의
                                    postModel.userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                                    postModel.writerUid = uid;
                                    postModel.profileImageUrl = String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl());
                                    postModel.imageUrl = task.getResult().getDownloadUrl().toString();
                                    postModel.content = content.getText().toString().trim();
                                    postModel.tags = tagline;
                                    postModel.timestamp = ServerValue.TIMESTAMP;
                                    postModel.star_count = "0";
                                    postModel.uniqueID = uniqueID;

                                    //태그 DB에 저장
                                    saveTag(postModel.uniqueID, postModel.timestamp);
                                    //회원포스트 DB에 저장
                                    aPost.uniqueID = postModel.uniqueID;
                                    aPost.writerUid = postModel.writerUid;
                                    aPost.timestamp = postModel.timestamp;
                                    aPost.imageUrl = postModel.imageUrl;
                                    accountPost();
                                    //DB에 저장
                                    FirebaseDatabase.getInstance().getReference().child("posts").child(uniqueID).setValue(postModel)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //다중 클릭 방지, progress 바
                                                    dialog.dismiss();
                                                    upload.setEnabled(true);

                                                    Toast.makeText(getActivity(), "Uploaded Successfully!", Toast.LENGTH_SHORT).show();

                                                    bottom.setSelectedItemId(R.id.action_home);

                                                    getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new HomeFragment()).commit();
                                                }
                                            });
                                }
                            });


                }


            }
        });


        return view;
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

    //회원 포스트 저장
    public void accountPost(){
        FirebaseDatabase.getInstance().getReference("accountPost").child(aPost.writerUid).child(aPost.uniqueID).setValue(aPost);
    }

    //이미지 올리기
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FROM_ALBUM && resultCode == RESULT_OK) {
            imageView.setImageURI(data.getData()); //뷰 바꿈
            imageUri = data.getData();              //이미지 경로 원본
        }
    }
}
