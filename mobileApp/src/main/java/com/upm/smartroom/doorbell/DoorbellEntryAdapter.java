package com.upm.smartroom.doorbell;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.upm.smartroom.GlideApp;
import com.upm.smartroom.R;

import java.util.ArrayList;

/**
 * RecyclerView adapter to populate doorbell entries from Firebase.
 */
public class DoorbellEntryAdapter extends FirebaseRecyclerAdapter<DoorbellEntry, DoorbellEntryAdapter.DoorbellEntryViewHolder> {

    /**
     * ViewHolder for each doorbell entry
     */
    public static class DoorbellEntryViewHolder extends RecyclerView.ViewHolder {

        public final ImageView image;
        public final TextView time;
        public final TextView metadata;

        public DoorbellEntryViewHolder(View itemView) {
            super(itemView);

            this.image = (ImageView) itemView.findViewById(R.id.imageView1);
            this.time = (TextView) itemView.findViewById(R.id.textView1);
            this.metadata = (TextView) itemView.findViewById(R.id.textView2);
        }
    }

    private Context mApplicationContext;
    private FirebaseStorage mFirebaseStorage;

    //当前view， ref databaseRef
    public DoorbellEntryAdapter(Context context, DatabaseReference ref) {
        super(new FirebaseRecyclerOptions.Builder<DoorbellEntry>()
                .setQuery(ref, DoorbellEntry.class)
                .build());

        mApplicationContext = context.getApplicationContext();
        mFirebaseStorage = FirebaseStorage.getInstance();
    }

    @Override
    public DoorbellEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View entryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.doorbell_entry, parent, false);

        return new DoorbellEntryViewHolder(entryView);
    }

    @Override
    protected void onBindViewHolder(DoorbellEntryViewHolder holder, int position, DoorbellEntry model) {
        // Display the timestamp
        //显示时间
        CharSequence prettyTime = DateUtils.getRelativeDateTimeString(mApplicationContext,
                model.getTimestamp(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        holder.time.setText(prettyTime);

        // Display the image
        //显示图片
        if (model.getImage() != null) {
            StorageReference imageRef = mFirebaseStorage.getReferenceFromUrl(model.getImage());

            GlideApp.with(mApplicationContext)
                    .load(imageRef)
                    .placeholder(R.drawable.ic_image)
                    .into(holder.image);
        }

        // Display the metadata
        //显示备注
        if (model.getAnnotations() != null) {
            ArrayList<String> keywords = new ArrayList<>(model.getAnnotations().keySet());

            int limit = Math.min(keywords.size(), 3);
            //设置
            holder.metadata.setText(TextUtils.join("\n", keywords.subList(0, limit)));
        } else {
            holder.metadata.setText("no annotations yet");

        }
    }
}
