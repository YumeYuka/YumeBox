package plus.yumeyuka.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import plus.yumeyuka.yumebox.core.util.createListFromParcelSlice
import plus.yumeyuka.yumebox.core.util.writeToParcelSlice

@Serializable
data class ProxyGroup(
    val type: Proxy.Type,
    val proxies: List<Proxy>,
    val now: String,
) : Parcelable {
    class SliceProxyList(data: List<Proxy>) : List<Proxy> by data, Parcelable {
        constructor(parcel: Parcel) : this(Proxy.createListFromParcelSlice(parcel, 0, 50))

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            writeToParcelSlice(dest, flags)
        }

        companion object CREATOR : Parcelable.Creator<SliceProxyList> {
            override fun createFromParcel(parcel: Parcel): SliceProxyList {
                return SliceProxyList(parcel)
            }

            override fun newArray(size: Int): Array<SliceProxyList?> {
                return arrayOfNulls(size)
            }
        }
    }

    constructor(parcel: Parcel) : this(
        Proxy.Type.entries[parcel.readInt()],
        SliceProxyList(parcel),
        parcel.readString()!!,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type.ordinal)
        SliceProxyList(proxies).writeToParcel(parcel, 0)
        parcel.writeString(now)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProxyGroup> {
        override fun createFromParcel(parcel: Parcel): ProxyGroup {
            return ProxyGroup(parcel)
        }

        override fun newArray(size: Int): Array<ProxyGroup?> {
            return arrayOfNulls(size)
        }
    }
}
