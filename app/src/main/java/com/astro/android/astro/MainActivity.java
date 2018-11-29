package com.astro.android.astro;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.astro.android.astro.fragment.AccountFragment;
import com.astro.android.astro.fragment.HomeFragment;
import com.astro.android.astro.fragment.SearchMainFragment;
import com.astro.android.astro.fragment.UploadFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static BottomNavigationView bottom;
    private String gotoAccount;
    private String targetUid;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //상단 상태바, 하단 네비게이션바 반투명
//        getWindow().setStatusBarColor(Color.parseColor("#20111111"));
//        getWindow().setNavigationBarColor(Color.parseColor("#20111111"));

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //바텀 네비 shift 모드 해제
        bottom = findViewById(R.id.mainactivity_bottom_navi);
        disableShiftMode(bottom);

        //쫒아내기
        byebye(myUid);

        //First view
        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new HomeFragment()).commit();

        //채팅에서 account 들어갈때
        gotoAccount = getIntent().getStringExtra("gotoAccount");
        if (gotoAccount != null) {
            targetUid = getIntent().getStringExtra("targetUid");
            Fragment account = new AccountFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("myPage", false);
            bundle.putString("userUid", "" + targetUid);
            bundle.putString("departure", "chat");
            account.setArguments(bundle);
            getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).commit();
        }

        //bottom navi
        bottom.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_home:
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new HomeFragment()).commit();
                        return true;
                    case R.id.action_upload:
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new UploadFragment()).commit();
                        return true;
                    case R.id.action_account:
                        //데이터 넣기
                        Fragment account = new AccountFragment();
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("myPage", true);
                        account.setArguments(bundle);

                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, account).commit();
                        return true;

                    case R.id.action_search:
                        getFragmentManager().beginTransaction().replace(R.id.mainactivity_framelayout, new SearchMainFragment()).commit();
                        return true;
                }
                return false;
            }
        });


        psaaPushTokenToServer();
    }

    //바텀 네비 shift 모드 해제
    @SuppressLint("RestrictedApi")
    public static void disableShiftMode(BottomNavigationView view) {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                //noinspection RestrictedApi
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                //noinspection RestrictedApi
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException e) {
            Log.e("BNVHelper", "Unable to get shift mode field", e);
        } catch (IllegalAccessException e) {
            Log.e("BNVHelper", "Unable to change value of shift mode", e);
        }
    }

    //Push 알림을 위한 토큰 업데이트
    void psaaPushTokenToServer() {

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String token = FirebaseInstanceId.getInstance().getToken();
        //HashMap으로 밖에 안됨 (파이어베이스)
        Map<String, Object> map = new HashMap<>();
        map.put("pushToken", token);

        FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map);

    }

    //리스너 생성 (프래그먼트에서 뒤로가기 감지)


    public interface OnBackPressedListener {
        void onBack();
    }

    private OnBackPressedListener mBackListener;

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        mBackListener = listener;
    }

    @Override
    public void onBackPressed() {

        if (mBackListener != null) {
            mBackListener.onBack();
            mBackListener = null;
        } else {
            super.onBackPressed();
        }
    }

    //쫒아 내기
    private void byebye(String uid) {
        FirebaseDatabase.getInstance().getReference("users").orderByChild("uid").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null) {
                            FirebaseAuth.getInstance().signOut();
                            finish();
                            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
}
