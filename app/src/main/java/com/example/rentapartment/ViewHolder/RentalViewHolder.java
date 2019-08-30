package com.example.rentapartment.ViewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentapartment.Interface.ItemClickListener;
import com.example.rentapartment.R;

public class RentalViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{

    public TextView txtRentalName, txtRentalDescription, txtRentalPrice;
    public ImageView imageView;
    public  ItemClickListener listener;

    public RentalViewHolder(@NonNull View itemView)
    {
        super(itemView);

        imageView = (ImageView) itemView.findViewById(R.id.rental_image);
        txtRentalName = (TextView) itemView.findViewById(R.id.rental_name);
        txtRentalDescription = (TextView) itemView.findViewById(R.id.rental_description);
        txtRentalPrice = (TextView) itemView.findViewById(R.id.rental_price);
    }

    public void  setItemClickListener(ItemClickListener listener)
    {
        this.listener = listener;
    }

    @Override
    public void onClick(View view)
    {
        listener.onClik(view, getAdapterPosition(), false);

    }
}
