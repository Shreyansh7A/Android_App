package edu.illinois.cs.cs124.ay2022.mp.models;

//import org.apache.commons.lang3.ObjectUtils;
import java.util.ArrayList;
import java.util.List;

/*
 * Model storing information about a place retrieved from the backend server.
 *
 * You will need to understand some of the code in this file and make changes starting with MP1.
 */
@SuppressWarnings("unused")
public final class Place {
  /*
   * The Jackson JSON serialization library that we are using requires an empty constructor.
   * So don't remove this!
   */
  public Place() {}

  public Place(
      final String setId,
      final String setName,
      final double setLatitude,
      final double setLongitude,
      final String setDescription,
      final String setType) {
    id = setId;
    name = setName;
    latitude = setLatitude;
    longitude = setLongitude;
    description = setDescription;
    type = setType;
  }

  // ID of the place
  private String id;
  private String type;
  public String getType() {
    return type;
  }
  public String getId() {
    return id;
  }

  // Name of the person who submitted this favorite place
  private String name;

  public String getName() {
    return name;
  }

  // Latitude and longitude of the place
  private double latitude;

  public double getLatitude() {
    return latitude;
  }

  private double longitude;

  public double getLongitude() {
    return longitude;
  }

  // Description of the place
  private String description;

  public String getDescription() {
    return description;
  }

  public static List<Place> search(final List<Place> places, final String search) {
    List<Place> out = new ArrayList<>();
    if (places == null || search == null) {
      throw new IllegalArgumentException();
    }
    if (places.size() == 0 || search.trim().length() == 0) {
      return places;
    }
    String des = null;
    for (Place p : places) {
      des = p.getDescription().toLowerCase();
      System.out.println(des);
      for (int i = 0; i < des.length(); i++) {
        char x = des.charAt(i);
        if (x == '.' || x == '!' || x == '?' || x == ',' || x == ';' || x == '/') {
          des = des.substring(0, i) + " " + des.substring(i + 1, des.length());
        } else if (!Character.isAlphabetic(x) && x != ' ' && !(Character.isDigit(x))) {
          des = des.substring(0, i) + des.substring(i + 1, des.length());
        }
      }
      System.out.println(des);
      String[] temp = des.split(" ");
      for (String s : temp) {
        if (s.equals(search.trim().toLowerCase())) {
          out.add(p);
          break;
        }
      }
    }
    return out;
  }
}
