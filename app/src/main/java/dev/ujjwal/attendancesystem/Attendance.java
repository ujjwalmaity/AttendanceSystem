package dev.ujjwal.attendancesystem;

public class Attendance {
    private String latitude;
    private String longitude;
    private String address;
    private String timestamp;
    private String image;

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
