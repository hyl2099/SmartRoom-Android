package com.upm.smartroom.plant;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.upm.smartroom.GlideApp;
import com.upm.smartroom.R;

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
        public final ImageButton itemEdit;
        public final ImageButton itemDelete;
        public final Switch waterSwitch;

        public PlantEntryViewHolder(View itemView) {
            super(itemView);
            this.image = (ImageView) itemView.findViewById(R.id.image);
            this.name = (TextView) itemView.findViewById(R.id.name);
            this.humidityNeed = (TextView) itemView.findViewById(R.id.humidityNeed);
//            this.temperature = (TextView) itemView.findViewById(R.id.temperature);
//            this.humidity = (TextView) itemView.findViewById(R.id.humidity);

            this.itemDelete = (ImageButton)itemView.findViewById(R.id.item_delete);
            this.itemEdit = (ImageButton)itemView.findViewById(R.id.item_edit);
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
    public PlantEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflating layout doorbell_entry.xml
        View entryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.plant_entry, parent, false);
        return new PlantEntryViewHolder(entryView);
    }

    @Override
    protected void onBindViewHolder(final PlantEntryViewHolder holder, final int position, PlantEntry model) {
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
        final String key= getRef(position).getKey();
        mWaterDatabaseReference = databaseRef.child(key).child("switchState");

        //when click item delete button
        holder.itemDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("TAG","DELETE`````````````````````````````````````````````````");
                databaseRef.child(key).removeValue();
                //此处还应删除storage里的图片。
                //后面在写
                //删除对话框有问题，Builder参数应该是activity的context，但是这里一直有问题，以后再研究。
//                new AlertDialog.Builder(PlantMainActivity.this)
//                        .setTitle("Delete Item")
//                        .setMessage("Sure")
//                        .setPositiveButton("OK",
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog,
//                                                    int whichButton) {
//                                    databaseRef.child(key).removeValue();
//                                }
//                            })
//                        .setNegativeButton("Cancel", null)
//                        .show();

            }
        });

        //when click item edit button
        holder.itemEdit.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ShowToast")
            @Override
            public void onClick(View v) {
                Log.e("TAG", "EDIT`````````````````````````````````````````````````");
//                mApplicationContext.startActivity(new Intent(mApplicationContext, AddItemActivity.class));
                Toast.makeText(mApplicationContext,"Not finished",Toast.LENGTH_SHORT);
            }
        });
        //when click the item show buttons
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                holder.itemDelete.setVisibility(View.VISIBLE);
                holder.itemEdit.setVisibility(View.VISIBLE);
            }
        });

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
