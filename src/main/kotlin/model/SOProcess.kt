package model

import java.time.LocalDateTime
import java.util.*

enum class Priority {
    LOW,
    MEDIUM,
    HIGH


}

data class SOProcess(val priority:Priority, val pid:String = UUID.randomUUID().toString()) {
    val createdDate: LocalDateTime = LocalDateTime.now()

}

