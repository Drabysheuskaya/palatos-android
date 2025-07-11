package com.example.PalaTos.Model

import android.os.Parcel
import android.os.Parcelable

data class CartItems(
    val foodName: String = "",
    val foodPrice: String = "",
    val foodDescription: String = "",
    val foodImage: String = "",
    val foodIngredients: String = "",
    val foodQuantity: Int = 0,
    val estimatedTime: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(foodName)
        parcel.writeString(foodPrice)
        parcel.writeString(foodDescription)
        parcel.writeString(foodImage)
        parcel.writeString(foodIngredients)
        parcel.writeInt(foodQuantity)
        parcel.writeInt(estimatedTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CartItems> {
        override fun createFromParcel(parcel: Parcel): CartItems {
            return CartItems(parcel)
        }

        override fun newArray(size: Int): Array<CartItems?> {
            return arrayOfNulls(size)
        }
    }
}
