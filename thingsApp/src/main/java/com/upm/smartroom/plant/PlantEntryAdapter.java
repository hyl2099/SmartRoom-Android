package com.upm.smartroom.plant;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.upm.smartroom.GlideApp;
import com.upm.smartroom.R;
import com.upm.smartroom.doorbell.DoorbellEntry;
import com.upm.smartroom.doorbell.DoorbellEntryAdapter;

import java.util.ArrayList;

public class PlantEntryAdapter extends FirebaseRecyclerAdapter<PlantEntry, PlantEntryAdapter.PlantEntryViewHolder> {

    /**
     * ViewHolder for each plant entry
     */
    public static class PlantEntryViewHolder extends RecyclerView.ViewHolder {

        public final ImageView image;
        public final TextView time;
        public final TextView metadata;

        public PlantEntryViewHolder(View itemView) {
            super(itemView);
            this.image = (ImageView) itemView.findViewById(R.id.imageView1);
            this.time = (TextView) itemView.findViewById(R.id.textView1);
            this.metadata = (TextView) itemView.findViewById(R.id.textView2);
        }

    }

    private Context mApplicationContext;
    private FirebaseStorage mFirebaseStorage;

    //当前view， ref databaseRef
    public PlantEntryAdapter(Context context, DatabaseReference ref) {
        super(new FirebaseRecyclerOptions.Builder<PlantEntry>()
                .setQuery(ref, PlantEntry.class)
                .build());

        mApplicationContext = context.getApplicationContext();
        mFirebaseStorage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public PlantEntryAdapter.PlantEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflating layout doorbell_entry.xml
        View entryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.plant_entry, parent, false);

        PlantEntryAdapter.PlantEntryViewHolder viewHolder = new PlantEntryAdapter.PlantEntryViewHolder(entryView);

        //return new PlantEntryViewHolder(entryView);
        return viewHolder;
    }


    @Override
    protected void onBindViewHolder(PlantEntryAdapter.PlantEntryViewHolder holder, int position, PlantEntry model) {
        // Display the timestamp
        //显示上次浇水时间
        CharSequence prettyTime = DateUtils.getRelativeDateTimeString(mApplicationContext,
                model.getTimestamp(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        holder.time.setText(prettyTime);

        // Display the image
        //显示植物图片
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

        //display temperature

        ////display humidity


        //display humidityNeed
    }
}
