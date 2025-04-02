package com.example.starsentinel.presentation;

public class GeofenceData {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private float radius;
    private String address;

    // Empty constructor needed for Gson
    public GeofenceData() {}

    public GeofenceData(String id, String name, double latitude, double longitude, float radius, String address) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.address = address;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}