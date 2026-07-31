package net.vibmc.world;

public class WeatherSystem {
    private String weather = "clear";

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public void tick() {
        // Lightweight weather hooks for future expansion.
    }

    public String weather() {
        return weather;
    }
}
