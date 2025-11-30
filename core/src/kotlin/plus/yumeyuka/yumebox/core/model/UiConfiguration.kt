package plus.yumeyuka.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import plus.yumeyuka.yumebox.core.util.Parcelizer

@Serializable
class UiConfiguration : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UiConfiguration> {
        override fun createFromParcel(parcel: Parcel): UiConfiguration {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<UiConfiguration?> {
            return arrayOfNulls(size)
        }
    }
}
