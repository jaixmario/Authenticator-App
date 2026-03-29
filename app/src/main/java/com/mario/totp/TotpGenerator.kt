package com.mario.totp

import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpGenerator {
    private const val ALGORITHM = "HmacSHA1"
    private const val TIME_STEP = 30L

    fun generateTotp(secret: String): String {
        return try {
            val base32 = Base32()
            val key = base32.decode(secret.uppercase())
            val time = System.currentTimeMillis() / 1000 / TIME_STEP
            
            val data = ByteBuffer.allocate(8).putLong(time).array()
            val signKey = SecretKeySpec(key, ALGORITHM)
            val mac = Mac.getInstance(ALGORITHM)
            mac.init(signKey)
            val hash = mac.doFinal(data)
            
            val offset = hash[hash.size - 1].toInt() and 0xf
            val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)
            
            val otp = binary % 10.toDouble().pow(6.0).toInt()
            otp.toString().padStart(6, '0')
        } catch (e: Exception) {
            "000000"
        }
    }

    fun getRemainingSeconds(): Int {
        return (TIME_STEP - (System.currentTimeMillis() / 1000 % TIME_STEP)).toInt()
    }
}
