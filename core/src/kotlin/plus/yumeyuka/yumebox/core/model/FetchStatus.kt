package plus.yumeyuka.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import plus.yumeyuka.yumebox.core.util.Parcelizer

@Serializable
data class FetchStatus(
    val action: Action,
    val args: List<String>,
    val progress: Int,
    val max: Int,
) : Parcelable {
    enum class Action {
        FetchConfiguration,
        FetchProviders,
        Verifying,
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), dest, this)
    }

    companion object CREATOR : Parcelable.Creator<FetchStatus> {
        override fun createFromParcel(parcel: Parcel): FetchStatus {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<FetchStatus?> {
            return arrayOfNulls(size)
        }
    }
}
