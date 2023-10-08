package edu.illinois.cs.cs124.ay2022.mp.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import edu.illinois.cs.cs124.ay2022.mp.R;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow;
//import edu.illinois.cs.cs124.ay2022.mp.network.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

/*
 * App main activity.
 * Started when the app is launched, based on the configuration in the Android Manifest
 * (AndroidManifest.xml).
 * Should display places on the map based on data retrieved from the server.
 *
 * You will need to understand some of the code here and make changes to complete most project
 * checkpoints.
 */
@SuppressWarnings("FieldCanBeLocal")
public final class MainActivity extends AppCompatActivity
    implements Consumer<ResultMightThrow<List<Place>>>, SearchView.OnQueryTextListener, MapEventsReceiver {
  // You may find this useful when adding logging
  private static final String TAG = MainActivity.class.getSimpleName();
  private boolean isRestaurantClicked = false;
  private boolean isAcademicClicked = false;
  private boolean isCafeClicked = false;
  private boolean isLeisureClicked = false;
  // Reference to the MapView, initialized in onCreate, handy to have in other places
  private MapView mapView = null;

  // Reference to Application instance, initialized in onCreate, handy to have in other places
  private FavoritePlacesApplication favoritePlacesApplication = null;

  // List of all places retrieved from the server, initially set to an empty list to avoid nulls
  private List<Place> allPlaces = new ArrayList<>();

  // ID of the currently open place, used to keep the same popup open when the list of places is
  // updated
  // null indicates no currently open popup
  private String openPlace = null;

  // Map boundaries, used to limit the scrollable area.
  // Our tile server does not provide tiles outside this geographic region.
  public static final double MAP_LIMIT_NORTH = 40.1741;
  public static final double MAP_LIMIT_SOUTH = 40.0247;
  public static final double MAP_LIMIT_WEST = -88.3331;
  public static final double MAP_LIMIT_EAST = -88.1433;

  // Max and default map zoom levels
  public static final double MAP_MIN_ZOOM = 12.0;
  public static final double MAP_DEFAULT_ZOOM = 17.0;

  /*
   * onCreate is the first method called when this activity is created.
   * Code here normally does a variety of setup tasks, and functions somewhat similarly to a
   * constructor.
   */
  @Override
  protected void onCreate(final Bundle unused) {
    super.onCreate(unused);

    // Store a reference to the application instance so that we can access it in other methods
    favoritePlacesApplication = (FavoritePlacesApplication) getApplication();

    // Load the layout for this activity and set the title
    setContentView(R.layout.activity_main);
    setTitle("Favorite Places");

    // Find the MapView component in the layout and configure it properly
    // Also save the reference for later use
    mapView = findViewById(R.id.map);

    SearchView searchView = findViewById(R.id.search);
    searchView.setOnQueryTextListener(this);

    // A OpenStreetMaps tile source provides the tiles that are used to render the map.
    // We use our own tile source with relatively-recent tiles for the Champaign-Urbana area, to
    // avoid adding load to existing OSM tile servers.
    mapView.setTileSource(
        new XYTileSource(
            "CS124", 12, 18, 256, ".png", new String[] {"https://tiles.cs124.org/tiles/"}));

    // Limit the map to the Champaign-Urbana area, which is also the only area that our tile server
    // can provide tiles for.
    mapView.setScrollableAreaLimitLatitude(MAP_LIMIT_NORTH, MAP_LIMIT_SOUTH, 0);
    mapView.setScrollableAreaLimitLongitude(MAP_LIMIT_WEST, MAP_LIMIT_EAST, 0);

    // Only allow zooming out so far
    mapView.setMinZoomLevel(MAP_MIN_ZOOM);

    // Set the current map zoom level to the default
    IMapController mapController = mapView.getController();
    mapController.setZoom(MAP_DEFAULT_ZOOM);
    mapController.setCenter(new GeoPoint(40.10986682167534, -88.22831928981661));
  }

  /*
   * onResume is called right before the activity begins interacting with the user.
   * So this is a good time to update our list of places.
   * We pass the MainActivity as the callback to the call to getPlaces, which is why this class
   * implements Consumer<ResultMightThrow<List<Place>>>, a functional interface allowing
   * our networking client to pass back the list of places to us once the network call completes.
   * We'll discuss this more when we talk about networking in Android on MP2.
   */
  @Override
  protected void onResume() {
    super.onResume();
    favoritePlacesApplication.getClient().getPlaces(this);
  }

  /*
   * Called by code in Client.java when the call to retrieve the list of places from the server
   * completes.
   * We save the full list of places and update the UI.
   * Note the use of the ResultMightThrow to have the exception thrown and caught here.
   * This is due to how Android networking requests are handled.
   * For a longer explanation, see the note on ResultMightThrow.java.
   */
  @Override
  public void accept(final ResultMightThrow<List<Place>> result) {
    // We use a try-catch because getResult throws if the result contains an exception
    try {
      // Save the list of all available places
      allPlaces = result.getResult();
      // Update the UI to show all available places
      updateShownPlaces(allPlaces);
    } catch (Exception e) {
      e.printStackTrace();
      Log.e(TAG, "getPlaces threw an exception: " + result.getException());
    }
  }

  /*
   * Update the list of places shown on the map.
   *
   * Helper method used to convert our List<Place> to a set of markers that will appear on the map
   * drawn by osmdroid.
   */
  private void updateShownPlaces(final List<Place> showPlaces) {
    /*
     * Go through all existing overlays that are markers and close their popups.
     * If we don't do this, updates to the list of places that are currently visible can leave
     * open popups that aren't connected to any marker.
     * This seems like a bug in osmdroid,
     * reported here: https://github.com/osmdroid/osmdroid/issues/1858.
     */
    for (int i = 0; i < mapView.getOverlays().size(); i++) {
      Overlay existing = mapView.getOverlays().get(i);
      if (!(existing instanceof Marker)) {
        continue;
      }
      Marker marker = (Marker) existing;
      marker.closeInfoWindow();
    }

    // Clear all overlays and the ID of the currently open info window
    mapView.getOverlays().clear();
    String newOpenPlace = null;


    Button restaurantView = findViewById(R.id.RestaurantView);
    Button academicView = findViewById(R.id.AcademicView);
    Button cafeView = findViewById(R.id.CafeView);
    Button leisureView = findViewById(R.id.LeisureView);
    List<Place> restaurants = new ArrayList<>();
    List<Place> cafe = new ArrayList<>();
    List<Place> leisure = new ArrayList<>();
    List<Place> academic = new ArrayList<>();
    for (Place restaurantPlace : allPlaces) {
      if (restaurantPlace.getType().equals("Restaurant")) {
        restaurants.add(restaurantPlace);
      }
    }
    for (Place cafePlace : allPlaces) {
      if (cafePlace.getType().equals("Cafe")) {
        cafe.add(cafePlace);
      }
    }
    for (Place leisurePlace : allPlaces) {
      if (leisurePlace.getType().equals("Leisure Spot")) {
        leisure.add(leisurePlace);
      }
    }
    for (Place academicPlace : allPlaces) {
      if (academicPlace.getType().equals("Academic Building")) {
        academic.add(academicPlace);
      }
    }
    Drawable defaultBackground = restaurantView.getBackground();
    // Create markers for each place in our list and add them to the map
    for (Place place : showPlaces) {
      // Create a new Marker

      Marker marker = new Marker(mapView);
      // Set the ID so that we can track which marker has an open popup
      marker.setId(place.getId());

      // Set the position and other attributes appropriately
      //marker.set
      marker.setPosition(new GeoPoint(place.getLatitude(), place.getLongitude()));
      marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
      marker.setTitle(place.getDescription() + "\n" + place.getType());
      restaurantView.setOnClickListener(v -> {
        if (isRestaurantClicked == false) {
          isRestaurantClicked = true;
          updateShownPlaces(restaurants);
          restaurantView.setBackgroundColor(0);
        } else if (isRestaurantClicked == true) {
          isRestaurantClicked = false;
          updateShownPlaces(allPlaces);
          restaurantView.setBackground(defaultBackground);
        }
        Log.d(TAG, "restaurantclick");

        /*if ((place.getType().equals("Cafe") || place.getType().equals("Academic Building")
            || place.getType().equals("Leisure Spot")) && isRestaurantClicked == true) {
          updateShownPlaces(restaurants);
          //marker.setVisible(false);
        } else {
          marker.setVisible(true);
        }*/

      });
      academicView.setOnClickListener(v -> {
        if (isAcademicClicked == false) {
          isAcademicClicked = true;
          updateShownPlaces(academic);
          academicView.setBackgroundColor(0);
        } else if (isAcademicClicked == true) {
          isAcademicClicked = false;
          updateShownPlaces(allPlaces);
          academicView.setBackground(defaultBackground);
        }
      });
      cafeView.setOnClickListener(v -> {
        if (isCafeClicked == false) {
          isCafeClicked = true;
          updateShownPlaces(cafe);
          cafeView.setBackgroundColor(0);
        } else if (isCafeClicked == true) {
          isCafeClicked = false;
          updateShownPlaces(allPlaces);
          cafeView.setBackground(defaultBackground);
        }
      });
      leisureView.setOnClickListener(v -> {
        if (isLeisureClicked == false) {
          isLeisureClicked = true;
          updateShownPlaces(leisure);
          leisureView.setBackgroundColor(0);
        } else if (isLeisureClicked == true) {
          isLeisureClicked = false;
          updateShownPlaces(allPlaces);
          leisureView.setBackground(defaultBackground);
        }
      });
      /*
       * Normally clicking on the marker both opens the popup and recenters the map.
       * The map recentering is a bit annoying, so we override this callback here to disable it.
       * The argument to setOnMarkerClickListener is just a lambda function called whenever the
       * marker is clicked.
       * This also allows us to track which marker was open.
       */
      marker.setOnMarkerClickListener(
          (m, unused) -> {
            if (!m.isInfoWindowShown()) {
              m.showInfoWindow();
              openPlace = m.getId();
            } else {
              m.closeInfoWindow();
              openPlace = null;
            }
            return true;
          });

      // Preserve the currently open place if there was one, and reopen the popup on the
      // appropriate marker
      if (marker.getId().equals(openPlace)) {
        marker.showInfoWindow();
        newOpenPlace = openPlace;
      }

      // Add the marker to the map
      mapView.getOverlays().add(marker);
    }

    // Update the currently-open marker
    // This will clear openPlace if the marker that was previously shown is no longer open
    openPlace = newOpenPlace;

    mapView.getOverlays().add(new MapEventsOverlay(this));

    // Force the MapView to redraw so that we see the updated list of markers
    mapView.invalidate();
  }

  @Override
  public boolean onQueryTextChange(final String text) {
    List<Place> searchList = Place.search(allPlaces, text);
    if (searchList.size() == 0) {
      updateShownPlaces(allPlaces);
    } else {
      updateShownPlaces(searchList);
      Log.d(TAG, "onQueryTextChange" + text);
    }
    return true;
  }
  @Override
  public boolean onQueryTextSubmit(final String text) {
    return false;
  }

  @Override
  public boolean singleTapConfirmedHelper(final GeoPoint p) {
    Log.d(TAG, "singleTap");
    return false;
  }

  @Override
  public boolean longPressHelper(final GeoPoint p) {
    Log.d(TAG, "longPress");
    Intent launchAddFavoritePlace = new Intent(this, AddPlaceActivity.class);
    String lat = String.valueOf(p.getLatitude());
    String lon = String.valueOf(p.getLongitude());
    launchAddFavoritePlace.putExtra("latitude", lat);
    launchAddFavoritePlace.putExtra("longitude", lon);
    startActivity(launchAddFavoritePlace);
    return false;
  }
}
