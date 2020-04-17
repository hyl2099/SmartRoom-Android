package com.upm.smartroom.plant;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.upm.smartroom.GlideApp;
import com.upm.smartroom.R;
import com.upm.smartroom.doorbell.DoorbellMsgActivity;


public class PlantEntryAdapter extends FirebaseRecyclerAdapter<PlantEntry, PlantEntryAdapter.PlantEntryViewHolder> {

    /**
     * ViewHolder for each plant entry
     */
    public static class PlantEntryViewHolder extends RecyclerView.ViewHolder {

        public final ImageView image;
        public final TextView name;
        public final TextView humidityNeed;
//        public final TextView temperature;
//        public final TextView humidity;
        public final ImageButton itemEdit;
        public final ImageButton itemDelete;


        public PlantEntryViewHolder(View itemView) {
            super(itemView);
            this.image = (ImageView) itemView.findViewById(R.id.image);
            this.name = (TextView) itemView.findViewById(R.id.name);
            this.humidityNeed = (TextView) itemView.findViewById(R.id.humidityNeed);
//            this.temperature = (TextView) itemView.findViewById(R.id.temperature);
//            this.humidity = (TextView) itemView.findViewById(R.id.humidity);

            this.itemDelete = (ImageButton)itemView.findViewById(R.id.item_delete);
            this.itemEdit = (ImageButton)itemView.findViewById(R.id.item_edit);
        }

    }

    private Context mApplicationContext;
    private FirebaseStorage mFirebaseStorage;
    private DatabaseReference databaseRef;

    //当前view， ref databaseRef
    public PlantEntryAdapter(Context context, DatabaseReference ref) {
        super(new FirebaseRecyclerOptions.Builder<PlantEntry>()
                .setQuery(ref, PlantEntry.class)
                .build());
        mApplicationContext = context.getApplicationContext();
        mFirebaseStorage = FirebaseStorage.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference().child("plants");
    }

    @NonNull
    @Override
    public PlantEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflating layout doorbell_entry.xml
        View entryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.plant_entry, parent, false);

        return new PlantEntryViewHolder(entryView);
    }

    @Override
    protected void onBindViewHolder(final PlantEntryViewHolder holder, int position, PlantEntry model) {
        // Display the timestamp
        //显示上次浇水时间
//        CharSequence prettyTime = DateUtils.getRelativeDateTimeString(mApplicationContext,
//                model.getTimestamp(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
//        holder.time.setText(prettyTime);

        // Display the image
        //显示植物图片
        if (model.getImage() != null) {
            StorageReference imageRef = mFirebaseStorage.getReferenceFromUrl(model.getImage());
            GlideApp.with(mApplicationContext)
                    .load(imageRef)
                    .placeholder(R.drawable.ic_image)
                    .into(holder.image);
        }
        //display name
        holder.name.setText(model.getName());
        //display temperature
//        holder.temperature.setText(model.getTemperature());
        ////display humidity
//        holder.humidity.setText(model.getHumidity());
        //display humidityNeed
        holder.humidityNeed.setText(model.getHumidityNeed());
    }

    private void deleteRef(String id) {
        databaseRef.child(id).removeValue();
    }

}
