package me.dantaeusb.zettergallery.network.http.dto;

public class PurchaseRequest {
    public int price;
    public int cycleId;

    public PurchaseRequest(int price, int cycleId) {
        this.price = price;
        this.cycleId = cycleId;
    }
}
