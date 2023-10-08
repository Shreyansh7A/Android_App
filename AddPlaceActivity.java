package edu.illinois.cs.cs124.ay2022.mp.activities;

//import static edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication.CLIENT_ID;
//import static edu.illinois.cs.cs124.ay2022.mp.network.Client.OBJECT_MAPPER;
import android.content.Intent;
//import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
//import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
//import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import edu.illinois.cs.cs124.ay2022.mp.R;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow;
import edu.illinois.cs.cs124.ay2022.mp.network.Client;

public class AddPlaceActivity extends AppCompatActivity implements Consumer<ResultMightThrow<Boolean>> {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String TAG = AddPlaceActivity.class.getSimpleName();
  private String type;
  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_addplace);

    Intent returnToMain = new Intent(this, MainActivity.class);
    returnToMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    Button cancelButton = findViewById(R.id.cancel_button);
    cancelButton.setOnClickListener(v -> {
      startActivity(returnToMain);
    });
    Intent savePlace = new Intent(this, MainActivity.class);
    Bundle extras = getIntent().getExtras();
    double lat = Double.parseDouble(extras.getString("latitude"));
    double lon = Double.parseDouble(extras.getString("longitude"));
    Button saveButton = findViewById(R.id.save_button);
    Button restaurantButton = findViewById(R.id.restaurant_button);
    Button academicButton = findViewById(R.id.Academic_Building_button);
    Button cafeButton = findViewById(R.id.Cafe_button);
    Button leisureButton = findViewById(R.id.Leisure_Spot_button);
    restaurantButton.setOnClickListener(v -> {
      type = "Restaurant";
      restaurantButton.setClickable(false);
      cafeButton.setClickable(false);
      leisureButton.setClickable(false);
      academicButton.setClickable(false);
      restaurantButton.setBackgroundColor(87687268);
    });
    academicButton.setOnClickListener(v -> {
      type = "Academic Building";
      restaurantButton.setClickable(false);
      cafeButton.setClickable(false);
      leisureButton.setClickable(false);
      academicButton.setClickable(false);
      academicButton.setBackgroundColor(87687268);
    });
    cafeButton.setOnClickListener(v -> {
      type = "Cafe";
      restaurantButton.setClickable(false);
      cafeButton.setClickable(false);
      leisureButton.setClickable(false);
      academicButton.setClickable(false);
      cafeButton.setBackgroundColor(0);
    });
    leisureButton.setOnClickListener(v -> {
      type = "Leisure Spot";
      restaurantButton.setClickable(false);
      cafeButton.setClickable(false);
      leisureButton.setClickable(false);
      academicButton.setClickable(false);
      leisureButton.setBackgroundColor(87687268);
    });
    saveButton.setOnClickListener(v -> {
      EditText desc = findViewById(R.id.description);
      String des = desc.getText().toString();
      Log.d(TAG, lon + des);
      Place newPlace = new Place(FavoritePlacesApplication.CLIENT_ID, "Shreyansh", lat, lon, des, type);
      Client.start().postFavoritePlace(newPlace, this);
      startActivity(savePlace);
    // I have to add my methods to keep description and everything here.
    });
  }

  @Override
  public void accept(final ResultMightThrow<Boolean> booleanResultMightThrow) {
  }
}
