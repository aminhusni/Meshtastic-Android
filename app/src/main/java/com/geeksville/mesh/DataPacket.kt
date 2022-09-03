package com.geeksville.mesh

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.nio.charset.Charset


@Parcelize
enum class MessageStatus : Parcelable {
    UNKNOWN, // Not set for this message
    RECEIVED, // Came in from the mesh
    QUEUED, // Waiting to send to the mesh as soon as we connect to the device
    ENROUTE, // Delivered to the radio, but no ACK or NAK received
    DELIVERED, // We received an ack
    ERROR // We received back a nak, message not delivered
}

/**
 * A parcelable version of the protobuf MeshPacket + Data subpacket.
 */
@Serializable
data class DataPacket(
    var to: String? = ID_BROADCAST, // a nodeID string, or ID_BROADCAST for broadcast
    val bytes: ByteArray?,
    val dataType: Int, // A port number for this packet (formerly called DataType, see portnums.proto for new usage instructions)
    var from: String? = ID_LOCAL, // a nodeID string, or ID_LOCAL for localhost
    var time: Long = System.currentTimeMillis(), // msecs since 1970
    var id: Int = 0, // 0 means unassigned
    var status: MessageStatus? = MessageStatus.UNKNOWN,
    var hopLimit: Int = 0,
    var channel: Int = 0, // channel index
) : Parcelable {

    /**
     * If there was an error with this message, this string describes what was wrong.
     */
    var errorMessage: String? = null

    /**
     * Syntactic sugar to make it easy to create text messages
     */
    constructor(to: String? = ID_BROADCAST, text: String) : this(
        to, text.toByteArray(utf8),
        Portnums.PortNum.TEXT_MESSAGE_APP_VALUE
    )

    /**
     * If this is a text message, return the string, otherwise null
     */
    val text: String?
        get() = if (dataType == Portnums.PortNum.TEXT_MESSAGE_APP_VALUE)
            bytes?.toString(utf8)
        else
            null

    // Autogenerated comparision, because we have a byte array

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.createByteArray(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readParcelable(MessageStatus::class.java.classLoader),
        parcel.readInt(),
        parcel.readInt(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataPacket

        if (from != other.from) return false
        if (to != other.to) return false
        if (channel != other.channel) return false
        if (time != other.time) return false
        if (id != other.id) return false
        if (dataType != other.dataType) return false
        if (!bytes!!.contentEquals(other.bytes!!)) return false
        if (status != other.status) return false
        if (hopLimit != other.hopLimit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + to.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + id
        result = 31 * result + dataType
        result = 31 * result + bytes!!.contentHashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + hopLimit
        result = 31 * result + channel
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(to)
        parcel.writeByteArray(bytes)
        parcel.writeInt(dataType)
        parcel.writeString(from)
        parcel.writeLong(time)
        parcel.writeInt(id)
        parcel.writeParcelable(status, flags)
        parcel.writeInt(hopLimit)
        parcel.writeInt(channel)
    }

    override fun describeContents(): Int {
        return 0
    }

    /// Update our object from our parcel (used for inout parameters
    fun readFromParcel(parcel: Parcel) {
        to = parcel.readString()
        parcel.createByteArray()
        parcel.readInt()
        from = parcel.readString()
        time = parcel.readLong()
        id = parcel.readInt()
        status = parcel.readParcelable(MessageStatus::class.java.classLoader)
        hopLimit = parcel.readInt()
        channel = parcel.readInt()
    }

    companion object CREATOR : Parcelable.Creator<DataPacket> {
        // Special node IDs that can be used for sending messages

        /** the Node ID for broadcast destinations */
        const val ID_BROADCAST = "^all"

        /** The Node ID for the local node - used for from when sender doesn't know our local node ID */
        const val ID_LOCAL = "^local"

        /// special broadcast address
        const val NODENUM_BROADCAST = (0xffffffff).toInt()

        fun nodeNumToDefaultId(n: Int): String = "!%08x".format(n)

        override fun createFromParcel(parcel: Parcel): DataPacket {
            return DataPacket(parcel)
        }

        override fun newArray(size: Int): Array<DataPacket?> {
            return arrayOfNulls(size)
        }

        val utf8: Charset = Charset.forName("UTF-8")
    }
}