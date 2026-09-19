package com.vayro.vehicles.dto;

import com.vayro.vehicles.entity.MaintenanceStatus;
import com.vayro.vehicles.entity.Vehicle;
import com.vayro.vehicles.entity.VehicleCategory;
import com.vayro.vehicles.entity.VehicleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class VehicleResponse {

    private String id;
    private String name;
    private String brand;
    private String model;
    private Integer year;
    private String vehicleType;
    private VehicleCategory category;
    private String vehicleClass;
    private String description;
    private Double pricePerDay;
    private Double pricePerHour;
    private String fuelType;
    private String transmission;
    private Integer seats;
    private String mileage;
    private String engine;
    private String power;
    private String location;
    private VehicleStatus status;
    private MaintenanceStatus maintenanceStatus;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private String maintenanceNotes;
    private List<String> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VehicleResponse() {
    }

    public static VehicleResponse fromEntity(Vehicle v) {
        if (v == null) return null;
        VehicleResponse res = new VehicleResponse();
        res.id = v.getId();
        res.name = v.getName();
        res.brand = v.getBrand();
        res.model = v.getModel();
        res.year = v.getYear();
        res.vehicleType = v.getVehicleType();
        res.category = v.getCategory();
        res.vehicleClass = v.getVehicleClass();
        res.description = v.getDescription();
        res.pricePerDay = v.getPricePerDay();
        res.pricePerHour = v.getPricePerHour();
        res.fuelType = v.getFuelType();
        res.transmission = v.getTransmission();
        res.seats = v.getSeats();
        res.mileage = v.getMileage();
        res.engine = v.getEngine();
        res.power = v.getPower();
        res.location = v.getLocation();
        res.status = v.getStatus();
        res.maintenanceStatus = v.getMaintenanceStatus();
        res.lastMaintenanceDate = v.getLastMaintenanceDate();
        res.nextMaintenanceDate = v.getNextMaintenanceDate();
        res.maintenanceNotes = v.getMaintenanceNotes();
        res.images = List.of(v.getImage1(), v.getImage2());
        res.createdAt = v.getCreatedAt();
        res.updatedAt = v.getUpdatedAt();
        return res;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public void setCategory(VehicleCategory category) {
        this.category = category;
    }

    public String getVehicleClass() {
        return vehicleClass;
    }

    public void setVehicleClass(String vehicleClass) {
        this.vehicleClass = vehicleClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(Double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public Double getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(Double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public String getMileage() {
        return mileage;
    }

    public void setMileage(String mileage) {
        this.mileage = mileage;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public MaintenanceStatus getMaintenanceStatus() {
        return maintenanceStatus;
    }

    public void setMaintenanceStatus(MaintenanceStatus maintenanceStatus) {
        this.maintenanceStatus = maintenanceStatus;
    }

    public LocalDate getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }

    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }

    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) {
        this.nextMaintenanceDate = nextMaintenanceDate;
    }

    public String getMaintenanceNotes() {
        return maintenanceNotes;
    }

    public void setMaintenanceNotes(String maintenanceNotes) {
        this.maintenanceNotes = maintenanceNotes;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
