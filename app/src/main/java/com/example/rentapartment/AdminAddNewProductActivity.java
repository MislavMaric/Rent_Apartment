package com.example.rentapartment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class AdminAddNewProductActivity extends AppCompatActivity {

    private  String CategoryName, Description, Price, Rname, saveCurrentDate, saveCurrentTime;
    private Button AddNewRentalButton;
    private ImageView InputRentalImage;
    private EditText InputRentalName, InputRentalDescription, InputRentalPrice;
    private static  final int GalleryPick = 1;
    private Uri ImageUri;
    private String productRandomKey, downloadImageUrl;
    private StorageReference RentalImagesRef; //ovdje su pohranjene sve slike oglasa
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


        InputRentalImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                OpenGallery();
            }
        });

        AddNewRentalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ValidateRentalData();
            }
        });
    }



    //korisnik odabire sliku za oglas:
    private void OpenGallery() {

        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GalleryPick); //rezultat odabira slike
    }

    //slika se prvo sprema u firebase storage, a potom u db

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==GalleryPick && resultCode==RESULT_OK  && data!=null)
        {
            ImageUri = data.getData();
            InputRentalImage.setImageURI(ImageUri); //prikaz odabrane slike
        }
    }

    //upisivanje podataka o oglasu
    private void ValidateRentalData() {

        Description = InputRentalDescription.getText().toString();
        Price = InputRentalPrice.getText().toString();
        Rname = InputRentalName.getText().toString();

        //ako korisnik ne odabere sliku,opis,cijenu ili ime onda se poavi toast:
        if (ImageUri == null)
        {
            Toast.makeText(this, "Rental image is required", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Description))
        {
            Toast.makeText(this, "Please write rental description", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Price))
        {
            Toast.makeText(this, "Please write rental price", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Rname))
        {
            Toast.makeText(this, "Please write rental name", Toast.LENGTH_SHORT).show();
        }
        else //ako je sve prije odabrano, onda se sprema rental na db
        {
            StoreRentalInformation();
        }
    }

    private void StoreRentalInformation()   //spremanje vremena i datuma kada je oglas objavljen
    {

        loadingBar.setTitle("Add new rental");
        loadingBar.setMessage("Please wait, adding new rental");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();


        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        //random key za pojedini oglas pomocu datuma i vremena:
        productRandomKey = saveCurrentDate + saveCurrentTime; //jedinstveni key za svaki oglas


        //prvo se pohrani slika u fb storage, zatim se sprema link slike u fb db, a zatim se prikazuje u app
        final StorageReference filePath = RentalImagesRef.child(ImageUri.getLastPathSegment() + productRandomKey + ".jpg"); //storage

        final UploadTask uploadTask = filePath.putFile(ImageUri);

        uploadTask.addOnFailureListener(new OnFailureListener() { //ako se dogodi pogreska, prikazat ce se u toastu
            @Override
            public void onFailure(@NonNull Exception e) {

                String message = e.toString();
                Toast.makeText(AdminAddNewProductActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() { //poruka da se slika uploadala uspjesno
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                Toast.makeText(AdminAddNewProductActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();

                //dohvacanje linka slike
                Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                        if (!task.isSuccessful())
                        {
                            throw task.getException();

                        }

                        downloadImageUrl = filePath.getDownloadUrl().toString(); //dohvacanje image uri, a ne link
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {

                        if (task.isSuccessful()) //ovdje se dohvaca link slike
                        {
                            downloadImageUrl = task.getResult().toString();

                            Toast.makeText(AdminAddNewProductActivity.this, "Got rental image url successfully", Toast.LENGTH_SHORT).show();

                            SaveRentalInfoToDatabase();
                        }
                    }
                });
            }
        });


    }


    private void SaveRentalInfoToDatabase() {

        HashMap<String, Object> productMap = new HashMap<>();
        productMap.put("pid", productRandomKey);
        productMap.put("date", saveCurrentDate);
        productMap.put("time", saveCurrentTime);
        productMap.put("description", Description);
        productMap.put("image", downloadImageUrl); //prosljeduje se image url
        productMap.put("category", CategoryName);
        productMap.put("price", Price);
        productMap.put("name", Rname);

        RentalsRef.child(productRandomKey).updateChildren(productMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful())
                        {
                            Intent intent = new Intent(AdminAddNewProductActivity.this, AdminCategoryActivity.class);
                            startActivity(intent);

                            loadingBar.dismiss();
                            Toast.makeText(AdminAddNewProductActivity.this, "Rental is added successfully", Toast.LENGTH_SHORT).show();
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
