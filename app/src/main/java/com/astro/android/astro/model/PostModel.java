package com.astro.android.astro.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostModel {

    public String userName;
    public String writerUid;
    public String profileImageUrl;
    public String imageUrl;
    public String content;
    public Object timestamp;
    public Map<String, Boolean> stars = new HashMap<>();
    public String star_count;
    public String tags;
    public String uniqueID;

    public Map<String, PostComment> postComments = new HashMap<>(); // 포스트 댓글

    public static class PostComment {

        public String profileImageUrl;
        public String uid;
        public String comment;
        public String username;
        public Object timestamp;

    }

}
