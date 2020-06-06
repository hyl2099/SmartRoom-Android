package com.upm.smartroom.postData;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Date;

public class SpringPicture {
    @SerializedName("owner")
    @Expose
    private String owner;
    @SerializedName("uploadTime")
    @Expose
    private Date uploadTime;
    @SerializedName("path")
    @Expose
    private String path;
    @SerializedName("photo")
    @Expose
    private byte[] photo;
    @SerializedName("image")
    @Expose
    private String image;
    @SerializedName("remark")
    @Expose
    private String remark;

    public SpringPicture() {
    }

    public SpringPicture(String owner, Date uploadTime, String path, byte[] photo, String image, String remark) {
        this.owner = owner;
        this.uploadTime = uploadTime;
        this.path = path;
        this.photo = photo;
        this.image = image;
        this.remark = remark;
    }

    public SpringPicture(SpringPicture p) {
        this.owner = p.owner;
        this.uploadTime = p.uploadTime;
        this.path = p.path;
        this.photo = p.photo;
        this.image = p.image;
        this.remark = p.remark;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "SpringPicture{" +
                "owner='" + owner + '\'' +
                ", uploadTime=" + uploadTime +
                ", path='" + path + '\'' +
                ", photo=" + Arrays.toString(photo) +
                ", image='" + image + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}
