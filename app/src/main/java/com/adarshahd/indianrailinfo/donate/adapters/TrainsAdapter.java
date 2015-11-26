package com.adarshahd.indianrailinfo.donate.adapters;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.adarshahd.indianrailinfo.donate.R;
import com.adarshahd.indianrailinfo.donate.fragments.TrainInfoFragment;
import com.adarshahd.indianrailinfo.donate.models.train.Train;
import com.adarshahd.indianrailinfo.donate.models.train.Trains;


/**
 * Created by ahd on 22/10/15.
 */
public class TrainsAdapter extends RecyclerView.Adapter<TrainsAdapter.ViewHolder> {

    private Trains trainList;
    private CheckAvailabilityListener mAvailabilityListener;
    private CheckFareListener mFareListener;

    public interface CheckAvailabilityListener {
        void onAvailabilityCheck(Integer position);
    }

    public interface CheckFareListener {
        void onFareCheck(Integer position);
    }

    public TrainsAdapter(Fragment fragment) {
        trainList = ((TrainInfoFragment) fragment).getTrains();
        mAvailabilityListener = (CheckAvailabilityListener) fragment;
        mFareListener = (CheckFareListener) fragment;
    }

    @Override
    public TrainsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trains,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TrainsAdapter.ViewHolder holder, final int position) {
        Train train = trainList.getTrains().get(position);
        holder.trainName.setText(train.getTrainName().replaceAll("[+#*]", ""));
        holder.trainNumber.setText(train.getTrainNumber().replaceAll("[+#*]", ""));
        holder.from.setText(train.getSource().replaceAll("[+#*]", ""));
        holder.to.setText(train.getDestination().replaceAll("[+#*]",""));
        holder.departure.setText(train.getDepartureTime());
        holder.arrival.setText(train.getArrivalTime());

        holder.getAvailability.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAvailabilityListener.onAvailabilityCheck(position);
            }
        });

        holder.getFare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFareListener.onFareCheck(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trainList.getTrains().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView trainName;
        private TextView trainNumber;
        private TextView from;
        private TextView to;
        private TextView departure;
        private TextView arrival;

        private Button getAvailability;
        private Button getFare;

        public ViewHolder(View itemView) {
            super(itemView);
            trainName = (TextView) itemView.findViewById(R.id.trainName);
            trainNumber = (TextView) itemView.findViewById(R.id.trainNumber);
            from = (TextView) itemView.findViewById(R.id.from);
            to = (TextView) itemView.findViewById(R.id.to);
            departure = (TextView) itemView.findViewById(R.id.departure);
            arrival = (TextView) itemView.findViewById(R.id.arrival);

            getAvailability = (Button) itemView.findViewById(R.id.getAvailability);
            getFare = (Button) itemView.findViewById(R.id.getFare);
        }
    }
}
