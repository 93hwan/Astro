package com.astro.android.astro;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.astro.android.astro.model.UserModel;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditAccountActivity extends AppCompatActivity {
    private static final int PICK_FROM_ALBUM = 11;
    private String userUid;
    private CircleImageView profile;
    private EditText name;
    private EditText bio;
    private Button button;
    private TextView limit;
    private int limit_num;
    private UserModel userModel;
    private ImageView back;
    private Uri imageUri;
    private Boolean changed_image;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);

        userUid = getIntent().getStringExtra("userUid");
        profile = findViewById(R.id.edit_account_imageview);
        name = findViewById(R.id.edit_account_name);
        bio = findViewById(R.id.edit_account_bio);
        button = findViewById(R.id.edit_account_button);
        limit = findViewById(R.id.edit_account_limit);
        back = findViewById(R.id.edit_account_back);

        //분류
        changed_image = false;

        //프로그래스바
        dialog = new ProgressDialog(EditAccountActivity.this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //데이터 불러오기
        FirebaseDatabase.getInstance().getReference("users").child(userUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        userModel = dataSnapshot.getValue(UserModel.class);
                        Glide.with(getApplicationContext())
                                .load(userModel.profileImageUrl)
                                .into(profile);
                        name.setText(userModel.name);
                        bio.setText(userModel.bio);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        //프로필사진 클릭시
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //사진 가져오기
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_ALBUM);
            }
        });

        //글자수 제한
        bio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                limit_num = 100;
                limit_num -= i2;
                limit.setText("" + limit_num);

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        //edit 클릭
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //다중 클릭 방지, progress 바
                button.setEnabled(false);
                dialog.show();
                dialog.setContentView(R.layout.item_progress);
                dialog.setCancelable(false);

                edit();
            }
        });


        //뒤로가기
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    //프로필 눌릴때 이미지 입력
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FROM_ALBUM && resultCode == RESULT_OK) {
            profile.setImageURI(data.getData());// 가운데 뷰를 바꿈
            imageUri = data.getData();          //이미지 경로 원본
            changed_image = true;
        }
    }

    //edit 클릭
    private void edit() {

        //이름 바꿨을때
        if (!userModel.name.equals(String.valueOf(name.getText()))) {
            userModel.name = String.valueOf(name.getText());
            //post 데이터에 이름 바꾸기
            FirebaseDatabase.getInstance().getReference("posts").orderByChild("writerUid").equalTo(userModel.uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                Map<String, Object> changedName = new HashMap<>();
                                changedName.put("userName", userModel.name);
                                FirebaseDatabase.getInstance().getReference("posts").child(item.getKey()).updateChildren(changedName);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        //bio 저장
        userModel.bio = String.valueOf(bio.getText());

        //프사 바꿨을때
        if (changed_image) {
            //이미지 저장소에 저장
            FirebaseStorage.getInstance().getReference().child("userImages").child(userModel.uid).putFile(imageUri)
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            String imageUrl = task.getResult().getDownloadUrl().toString();
                            userModel.profileImageUrl = imageUrl;

                            //회원에 프로필 사진 uri저장
                            UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(Uri.parse(imageUrl)).build();
                            FirebaseAuth.getInstance().getCurrentUser().updateProfile(userProfileChangeRequest);

                            //DB 저장
                            FirebaseDatabase.getInstance().getReference().child("users").child(userModel.uid).setValue(userModel)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            //다중 클릭 방지, progress 바
                                            dialog.dismiss();
                                            button.setEnabled(true);

                                            Toast.makeText(getApplicationContext(), "Edited", Toast.LENGTH_LONG).show();

                                            finish();
                                        }


                                    });
                        }
                    });
        }else{
            //DB 저장
            FirebaseDatabase.getInstance().getReference().child("users").child(userModel.uid).setValue(userModel)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //다중 클릭 방지, progress 바
                            dialog.dismiss();
                            button.setEnabled(true);

                            Toast.makeText(getApplicationContext(), "Edited", Toast.LENGTH_LONG).show();

                            finish();
                        }


                    });
        }



    }
}
