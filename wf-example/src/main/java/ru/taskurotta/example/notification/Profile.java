package ru.taskurotta.example.notification;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 15:29
 */
public class Profile {

    public static enum DELIVERY_TYPE {SMS, EMAIL, BLOCKED}

    private long id;
    private String email;
    private String phoneNumber;
    private DELIVERY_TYPE deliveryType = DELIVERY_TYPE.EMAIL;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public DELIVERY_TYPE getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(DELIVERY_TYPE deliveryType) {
        this.deliveryType = deliveryType;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", deliveryType='" + deliveryType + '\'' +
                '}';
    }
}
