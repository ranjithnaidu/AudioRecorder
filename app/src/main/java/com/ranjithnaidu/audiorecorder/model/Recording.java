package com.ranjithnaidu.audiorecorder.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Recording implements Parcelable {

    @NonNull
    private String path;

    private long length;

    // Constructor for existing Recording (it already has an id).
    public Recording(@NonNull String path, long length) {
        this.path = path;
        this.length = length;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    public void setPath(@NonNull String path) {
        this.path = path;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    // Implementation of Parcelable interface.
    protected Recording(Parcel in) {
        path = in.readString();
        length = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeLong(length);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Recording> CREATOR = new Parcelable.Creator<Recording>() {
        @Override
        public Recording createFromParcel(Parcel in) {
            return new Recording(in);
        }

        @Override
        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };
}
