package com.kaisoku.seydou.tryfirebase;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder>{

    public Context context;
    public List<BlogPost> blog_lists;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    public BlogRecyclerAdapter(List<BlogPost> blog_lists){
        this.blog_lists=blog_lists;
        firebaseFirestore= FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public ViewHolder onCreateViewHolder( ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item,parent,false);
        context = parent.getContext();

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
            viewHolder.setIsRecyclable(false);
            String desc_data = blog_lists.get(i).getDesc();
            String image_url = blog_lists.get(i).getImage_url();
            String user_id = blog_lists.get(i).getUser_id();
            final String blogPostId = blog_lists.get(i).BlogPostId;
            final String current_user_id = firebaseAuth.getCurrentUser().getUid();



            firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        String useName = task.getResult().getString("name");
                        String im_url = task.getResult().getString("image");

                        viewHolder.setUserData(useName,im_url);
                    }else {
                        Toast.makeText(context,"(Error list blog): "+task.getException().getMessage(),Toast.LENGTH_LONG).show();
                    }
                }
            });

            try {
                long millisecond = blog_lists.get(i).getTimestamp().getTime();
                String dateString = DateFormat.format("MM/dd/yyyy", new Date(millisecond)).toString();
                viewHolder.setTime(dateString);
            } catch (Exception e) {

                Toast.makeText(context, "Exception : " + e.getMessage(), Toast.LENGTH_SHORT).show();

            }

            //count like
            firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    if(!queryDocumentSnapshots.isEmpty()){
                        int count = queryDocumentSnapshots.size();
                        viewHolder.updatelike(count);
                    }else {
                        viewHolder.updatelike(0);
                    }
                }
            });

            // count comments
            firebaseFirestore.collection("Posts/"+blogPostId+"/Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    if(!queryDocumentSnapshots.isEmpty()){
                        int count = queryDocumentSnapshots.size();
                        viewHolder.updateCommentsCount(count);
                    }else {
                        viewHolder.updateCommentsCount(0);
                    }
                }
            });

            viewHolder.setBlogImage(image_url);
            viewHolder.setDescText(desc_data);

            //btn like cliked
            firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(current_user_id).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    if(documentSnapshot.exists()){
                        viewHolder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_accent));
                    }else {
                        viewHolder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_gray));

                    }
                }
            });

        viewHolder.blogCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent commentIntent = new Intent(context, CommentsActivity.class);
                commentIntent.putExtra("blog_post_id", blogPostId);
                context.startActivity(commentIntent);

            }
        });

            viewHolder.blogLikeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                            //like
                    firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(current_user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(!task.getResult().exists()){
                                Map<String,Object> likeMap = new HashMap<>();
                                likeMap.put("timestamp",FieldValue.serverTimestamp());
                                firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(current_user_id).set(likeMap);
                            }else {

                                firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(current_user_id).delete();

                            }
                        }
                    });

                }
            });


    }

    @Override
    public int getItemCount() {
        return blog_lists.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private View mView;

        private TextView descView;
        private ImageView blogImageView;
        private CircleImageView userImageView;
        private TextView userNameView;
        private TextView blogDate;
        private ImageView blogLikeBtn;
        private TextView BlogLikeCount;
        private ImageView blogCommentBtn;
        private TextView commentsCountView;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            blogLikeBtn = mView.findViewById(R.id.blog_like_btn);
            blogCommentBtn = mView.findViewById(R.id.blog_comment_icon);
        }

        public void setDescText(String desctext) {
            this.descView = mView.findViewById(R.id.blog_desc);
            descView.setText(desctext);
        }

        public void setBlogImage(String DownloadUri) {
            this.blogImageView = mView.findViewById(R.id.blog_imge);
            Glide.with(context).load(DownloadUri).into(blogImageView);
        }

        public void setUserData(String userName,String posts_url){
            this.userImageView = mView.findViewById(R.id.blog_user_img);

            Glide.with(context.getApplicationContext()).load(posts_url).into(userImageView);

            this.userNameView = mView.findViewById(R.id.blog_user_name);
            userNameView.setText(userName);
        }

        public void setTime(String date) {

            blogDate = mView.findViewById(R.id.blog_post_date);
            blogDate.setText(date);

        }

        public void updatelike(int count){
            BlogLikeCount = mView.findViewById(R.id.blog_like_count);
            BlogLikeCount.setText(count+" J'aime");
        }

        public void updateCommentsCount(int count){
            commentsCountView = mView.findViewById(R.id.blog_comment_count);
            commentsCountView.setText(count+" comments");
        }

    }
}
