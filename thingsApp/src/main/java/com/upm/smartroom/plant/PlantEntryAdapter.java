package com.upm.smartroom.plant;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.upm.smartroom.GlideApp;
import com.upm.smartroom.R;
import com.upm.smartroom.doorbell.DoorbellEntry;
import com.upm.smartroom.doorbell.DoorbellEntryAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class PlantEntryAdapter extends FirebaseRecyclerAdapter<PlantEntry, PlantEntryAdapter.PlantEntryViewHolder> {

    protected abstract void PlantEntryViewHolder(PlantEntryViewHolder viewHolder, PlantEntry model, int position);

    /**
     * ViewHolder for each plant entry
     */
    public static class PlantEntryViewHolder extends RecyclerView.ViewHolder {

        public final ImageView image;
        public final TextView name;
        public final TextView humidityNeed;
        //        public final TextView temperature;
//        public final TextView humidity;
        public final Switch waterSwitch;

        public PlantEntryViewHolder(View itemView) {
            super(itemView);
            this.image = (ImageView) itemView.findViewById(R.id.image);
            this.name = (TextView) itemView.findViewById(R.id.name);
            this.humidityNeed = (TextView) itemView.findViewById(R.id.humidityNeed);
//            this.temperature = (TextView) itemView.findViewById(R.id.temperature);
//            this.humidity = (TextView) itemView.findViewById(R.id.humidity);
            this.waterSwitch = (Switch)itemView.findViewById(R.id.waterSwitch);
        }

    }
    private Context mApplicationContext;
    private FirebaseStorage mFirebaseStorage;
    private DatabaseReference databaseRef;
    private DatabaseReference mWaterDatabaseReference;

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
    public PlantEntryAdapter.PlantEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflating layout doorbell_entry.xml
        View entryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.plant_entry, parent, false);

        PlantEntryAdapter.PlantEntryViewHolder viewHolder = new PlantEntryAdapter.PlantEntryViewHolder(entryView);
        return viewHolder;
    }


    @Override
    protected void onBindViewHolder(final PlantEntryAdapter.PlantEntryViewHolder holder, int position, PlantEntry model) {
        if (model.getImage() != null) {
            StorageReference imageRef = mFirebaseStorage.getReferenceFromUrl(model.getImage());
            GlideApp.with(mApplicationContext)
                    .load(imageRef)
                    .placeholder(R.drawable.ic_image)
                    .into(holder.image);
        }
        //display name
        holder.name.setText(model.getName());
        holder.humidityNeed.setText(model.getHumidityNeed());
        final String key= getRef(position).getKey();
        mWaterDatabaseReference = databaseRef.child(key).child("switchState");

        holder.waterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mWaterDatabaseReference.getKey(), "1");
                    databaseRef.child(key).updateChildren(childUpdates);
                    Log.d("WATER", "switch check Switch is on!!!");
                    holder.waterSwitch.setChecked(true);
                }else {
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mWaterDatabaseReference.getKey(), "0");
                    databaseRef.child(key).updateChildren(childUpdates);
                    Log.d("WATER", "switch check Switch is off!!!");
                    holder.waterSwitch.setChecked(false);
                }
            }
        });

    }
}
