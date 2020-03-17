package com.funbridge.server.pay.data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "pay_order")
@NamedQueries({
        @NamedQuery(name = "payOrder.getForLocalOrderNo", query = "select p from PayOrder p where p.localOrderNo=:localOrderNo")
})
public class PayOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "uid", length = 200, nullable = false)
    private long uId;

    @Column(name = "order_type", length = 200, nullable = false)
    private int orderType ;

    @Column(name = "local_order_no", length = 200, nullable = false)
    private String localOrderNo ;

    @Column(name = "local_order_time")
    private Date localOrderTime ;

    @Column(name = "order_no", length = 200)
    private String orderNo ;

    @Column(name = "deal_time")
    private Date dealTime ;

    @Column(name = "userid", length = 200, nullable = false)
    private String userId ;

    @Column(name = "shopuid", length = 200, nullable = false)
    private int shopuId ;

    @Column(name = "amount", length = 200, nullable = false)
    private int amount ;

    @Column(name = "order_status", length = 200, nullable = false)
    private int ordeStatus ;

    @Column(name = "extra_detail", length = 255)
    private String extraDetail ;

    public long getuId() {
        return uId;
    }

    public void setuId(long uId) {
        this.uId = uId;
    }

    public int getOrderType() {
        return orderType;
    }

    public void setOrderType(int orderType) {
        this.orderType = orderType;
    }

    public String getLocalOrderNo() {
        return localOrderNo;
    }

    public void setLocalOrderNo(String localOrderNo) {
        this.localOrderNo = localOrderNo;
    }

    public Date getLocalOrderTime() {
        return localOrderTime;
    }

    public void setLocalOrderTime(Date localOrderTime) {
        this.localOrderTime = localOrderTime;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Date getDealTime() {
        return dealTime;
    }

    public void setDealTime(Date dealTime) {
        this.dealTime = dealTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getShopuId() {
        return shopuId;
    }

    public void setShopuId(int shopuId) {
        this.shopuId = shopuId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getOrdeStatus() {
        return ordeStatus;
    }

    public void setOrdeStatus(int ordeStatus) {
        this.ordeStatus = ordeStatus;
    }

    public String getExtraDetail() {
        return extraDetail;
    }

    public void setExtraDetail(String extraDetail) {
        this.extraDetail = extraDetail;
    }


}
