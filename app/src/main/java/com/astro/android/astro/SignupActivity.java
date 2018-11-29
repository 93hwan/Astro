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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.astro.android.astro.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;


public class SignupActivity extends AppCompatActivity {

    private static final int PICK_FROM_ALBUM = 10;
    private EditText name;
    private EditText email;
    private EditText pw;
    private Button signup;
    private ImageView profile;
    private Uri imageUri;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);


        //변수 정의
        name = findViewById(R.id.signupActivity_edittext_name);
        email = findViewById(R.id.signupActivity_edittext_email);
        pw = findViewById(R.id.signupActivity_edittext_pw);
        signup = findViewById(R.id.signupActivity_button_signup);
        profile = findViewById(R.id.signupActivity_imageview_profile);

        //프로필 눌릴때
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //사진 가져오기
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_ALBUM);
            }
        });


        //프로그래스바
        dialog = new ProgressDialog(SignupActivity.this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


        //회원 가입
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //빈칸 방지 ( 기타 조건 충족 안될시 리턴)
                if (name.getText().toString().trim().equals("") || email.getText().toString().trim().equals("") ||
                        pw.getText().toString().trim().equals("")) {
                    Toast.makeText(getApplicationContext(), "Anything can't be blank", Toast.LENGTH_SHORT).show();
                    return;
                } else if (imageUri == null) {//사진 없을때
                    Toast.makeText(getApplicationContext(), "Add your picture!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!email.getText().toString().contains("@")) {//이메일 형식 안맞을때
                    Toast.makeText(getApplicationContext(), "Follow the email form", Toast.LENGTH_SHORT).show();
                    return;
                } else if (pw.getText().toString().trim().length() < 6) {//비밀번호 6자리 이상
                    Toast.makeText(getApplicationContext(), "Type password more than 6", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    //다중 클릭 방지, progress 바
                    signup.setEnabled(false);
                    dialog.show();
                    dialog.setContentView(R.layout.item_progress);
                    dialog.setCancelable(false);

                    //회원 가입
                    FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email.getText().toString().trim(), pw.getText().toString().trim())
                            .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    //User 기입
                                    final UserModel userModel = new UserModel();

                                    //회원에 이름 저장
                                    UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name.getText().toString()).build();
                                    task.getResult().getUser().updateProfile(userProfileChangeRequest);

                                    //정의
                                    userModel.name = name.getText().toString().trim();
                                    userModel.email = email.getText().toString().trim();
                                    userModel.pw = pw.getText().toString().trim();
                                    userModel.uid = task.getResult().getUser().getUid();
                                    userModel.bio="";

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

                                                    //DB에 저장
                                                    FirebaseDatabase.getInstance().getReference().child("users").child(userModel.uid).setValue(userModel)
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    //다중 클릭 방지, progress 바
                                                                    dialog.dismiss();
                                                                    signup.setEnabled(true);

                                                                    Toast.makeText(getApplicationContext(), "Signed up Successfully!", Toast.LENGTH_LONG).show();
                                                                    finish();
                                                                }
                                                            });
                                                }
                                            });


                                }
                            });
                }

            }
        });
    }

    //프로필 눌릴때 이미지 입력
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FROM_ALBUM && resultCode == RESULT_OK) {
            profile.setImageURI(data.getData());// 가운데 뷰를 바꿈
            profile.setBackground(null);
            imageUri = data.getData();          //이미지 경로 원본
        }
    }
}
