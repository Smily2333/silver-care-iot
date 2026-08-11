package com.silvercare.iot.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "location_address_cache")
public class LocationAddressCache {

    @Id
    @Column(length = 64)
    private String gridKey;

    @Column(nullable = false, precision = 11, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 12, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false, length = 255)
    private String approximateAddress;

    @Column(length = 128)
    private String road;

    @Column(length = 128)
    private String neighbourhood;

    @Column(length = 128)
    private String district;

    @Column(length = 128)
    private String city;

    @Column(length = 512)
    private String displayName;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public String getGridKey() {
        return gridKey;
    }

    public void setGridKey(String gridKey) {
        this.gridKey = gridKey;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getApproximateAddress() {
        return approximateAddress;
    }

    public void setApproximateAddress(String approximateAddress) {
        this.approximateAddress = approximateAddress;
    }

    public String getRoad() {
        return road;
    }

    public void setRoad(String road) {
        this.road = road;
    }

    public String getNeighbourhood() {
        return neighbourhood;
    }

    public void setNeighbourhood(String neighbourhood) {
        this.neighbourhood = neighbourhood;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
