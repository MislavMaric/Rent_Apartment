package com.example.rentapartment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rentapartment.Model.Rentals;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class RentalDetailsActivity extends AppCompatActivity
{
    private ImageView rentalImage;
    private TextView rentalPrice, rentalName, rentalDescription;
    private String rentalID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_details);
        
        rentalID = getIntent().getStringExtra("pid");//dohvacanje id u ovom activityu

        rentalImage = (ImageView) findViewById(R.id.rental_image_details);
        rentalPrice = (TextView) findViewById(R.id.rental_price_details);
        rentalName = (TextView) findViewById(R.id.rental_name_details);
        rentalDescription = (TextView) findViewById(R.id.rental_description_details);
        
        
        getRentalDetails(rentalID);//uzimanje detalja oglasa
    }

    private void getRentalDetails(String rentalID)
    {
        DatabaseReference rentalsRef = FirebaseDatabase.getInstance().getReference().child("Rentals");

        rentalsRef.child(rentalID).addValueEventListener(new ValueEventListener() //trazenje specificnog oglasa
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    Rentals rentals = dataSnapshot.getValue(Rentals.class);
                    //postavljanje imena, cijene...
                    rentalName.setText(rentals.getName());
                    rentalPrice.setText(rentals.getPrice());
                    rentalDescription.setText(rentals.getDescription());
                    Picasso.get().load(rentals.getImage()).into(rentalImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }
}
