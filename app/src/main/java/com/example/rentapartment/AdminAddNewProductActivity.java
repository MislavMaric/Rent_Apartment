package com.example.rentapartment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;



    public class AdminAddNewProductActivity extends AppCompatActivity
    {
        private String CategoryName, Description, Price, Rname, saveCurrentDate, saveCurrentTime;
        private Button AddNewRentalButton, TakePhoto, SelectPhoto;
        private ImageView InputRentalImage;
        private EditText InputRentalName, InputRentalDescription, InputRentalPrice;
        private static final int GalleryPick = 1;
        private Uri ImageUri;
        private String productRandomKey, downloadImageUrl;
        private StorageReference RentalImagesRef;
        private DatabaseReference RentalsRef;
        private ProgressDialog loadingBar;


        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_admin_add_new_product);


            CategoryName = getIntent().getExtras().get("category").toString();
            RentalImagesRef = FirebaseStorage.getInstance().getReference().child("Rental Images");
            RentalsRef = FirebaseDatabase.getInstance().getReference().child("Rentals");


            AddNewRentalButton = (Button) findViewById(R.id.add_new_rental);
            InputRentalImage = (ImageView) findViewById(R.id.select_rental_image);
            InputRentalName = (EditText) findViewById(R.id.rental_name);
            InputRentalDescription = (EditText) findViewById(R.id.rental_description);
            InputRentalPrice = (EditText) findViewById(R.id.rental_price);
            loadingBar = new ProgressDialog(this);

            SelectPhoto = (Button) findViewById(R.id.select_image_btn);
            TakePhoto = (Button) findViewById(R.id.take_image_btn);


            SelectPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    OpenGallery();
                }
            });

            TakePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent intent = new Intent(AdminAddNewProductActivity.this, CameraActivity.class);
                    startActivity(intent);
                }
            });


            AddNewRentalButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    ValidateProductData();
                }
            });
        }



        private void OpenGallery()
        {
            Intent galleryIntent = new Intent();
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GalleryPick);
        }


        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data)
        {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode==GalleryPick  &&  resultCode==RESULT_OK  &&  data!=null)
            {
                ImageUri = data.getData();
                InputRentalImage.setImageURI(ImageUri);
            }
        }


        private void ValidateProductData()
        {
            Description = InputRentalDescription.getText().toString();
            Price = InputRentalPrice.getText().toString();
            Rname = InputRentalName.getText().toString();


            if (ImageUri == null)
            {
                Toast.makeText(this, "Product image is mandatory...", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(Description))
            {
                Toast.makeText(this, "Please write product description...", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(Price))
            {
                Toast.makeText(this, "Please write product Price...", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(Rname))
            {
                Toast.makeText(this, "Please write product name...", Toast.LENGTH_SHORT).show();
            }
            else
            {
                StoreRentalInformation();
            }
        }



        private void StoreRentalInformation()
        {
            loadingBar.setTitle("Add New Rental");
            loadingBar.setMessage("Please wait, adding new rental");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            Calendar calendar = Calendar.getInstance();

            SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
            saveCurrentDate = currentDate.format(calendar.getTime());

            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
            saveCurrentTime = currentTime.format(calendar.getTime());

            productRandomKey = saveCurrentDate + saveCurrentTime;


            final StorageReference filePath = RentalImagesRef.child(ImageUri.getLastPathSegment() + productRandomKey + ".jpg");

            final UploadTask uploadTask = filePath.putFile(ImageUri);


            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    String message = e.toString();
                    Toast.makeText(AdminAddNewProductActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                {
                    Toast.makeText(AdminAddNewProductActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();

                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception
                        {
                            if (!task.isSuccessful())
                            {
                                throw task.getException();
                            }

                            downloadImageUrl = filePath.getDownloadUrl().toString();
                            return filePath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task)
                        {
                            if (task.isSuccessful())
                            {
                                downloadImageUrl = task.getResult().toString();

                                Toast.makeText(AdminAddNewProductActivity.this, "Got rental image url successfully", Toast.LENGTH_SHORT).show();

                                SaveProductInfoToDatabase();
                            }
                        }
                    });
                }
            });
        }



        private void SaveProductInfoToDatabase()
        {
            HashMap<String, Object> productMap = new HashMap<>();
            productMap.put("pid", productRandomKey);
            productMap.put("date", saveCurrentDate);
            productMap.put("time", saveCurrentTime);
            productMap.put("description", Description);
            productMap.put("image", downloadImageUrl);
            productMap.put("category", CategoryName);
            productMap.put("price", Price);
            productMap.put("name", Rname);

            RentalsRef.child(productRandomKey).updateChildren(productMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if (task.isSuccessful())
                            {
                                Intent intent = new Intent(AdminAddNewProductActivity.this, AdminCategoryActivity.class);
                                startActivity(intent);

                                loadingBar.dismiss();
                                Toast.makeText(AdminAddNewProductActivity.this, "Product is added successfully..", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                loadingBar.dismiss();
                                String message = task.getException().toString();
                                Toast.makeText(AdminAddNewProductActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
