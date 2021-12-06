package manager

import model.Priority
import model.SOProcess
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

private const val BUNDLE_NAME = "config"
private const val BUNDLE_PROPERTY_NAME = "max_capacity"
private val MAX_CAPACITY:Long = ResourceBundle.getBundle(BUNDLE_NAME).getString(BUNDLE_PROPERTY_NAME).toLong()
private val READ_WRITE_LOCK = ReentrantReadWriteLock()
private val WRiTE_LOCK: Lock = READ_WRITE_LOCK.writeLock()
private val READ_LOCK: Lock = READ_WRITE_LOCK.readLock()

private fun <T> lockWrite(another:() -> T): T {
    WRiTE_LOCK.lock()
    val res = another()
    WRiTE_LOCK.unlock()
    return res
}

private fun <T> lockRead(another:() -> T): T {
    READ_LOCK.lock()
    val res = another()
    READ_LOCK.unlock()
    return res
}

sealed interface AddProcess {
    val stateAllProcess: MutableSet<SOProcess>
    fun add(process: SOProcess): Boolean
}

class AddProcessDefault(private val maxCapacity:Long) : AddProcess {

    override val stateAllProcess: MutableSet<SOProcess> = HashSet<SOProcess>()
    override fun add(process: SOProcess): Boolean {
        return lockWrite{ if(stateAllProcess.size < maxCapacity) {
            stateAllProcess.add(process)
            true
        } else false }
    }
}

class AddProcessFiFo(private val maxCapacity:Long) : AddProcess {

    override val stateAllProcess: MutableSet<SOProcess> = LinkedHashSet<SOProcess>()
    override fun add(process: SOProcess): Boolean {
        lockWrite {
            stateAllProcess.add(process)
            val first = stateAllProcess.first()
            if(stateAllProcess.size > maxCapacity) stateAllProcess.remove(first);
        }
        return true
    }
}

class AddProcessPriority(private val maxCapacity:Long) : AddProcess {

    override val stateAllProcess: MutableSet<SOProcess> = TreeSet{ s1, s2 ->
        val comparePriority = s1.priority.compareTo(s2.priority)
        if(comparePriority != 0) comparePriority
        else s1.createdDate.compareTo(s2.createdDate)

    }
    override fun add(process: SOProcess): Boolean {
        return lockWrite {
            if(stateAllProcess.size < maxCapacity) stateAllProcess.add(process)
            else {
                val lessPriorityProcess =stateAllProcess.first()
                if(process.priority > lessPriorityProcess.priority) {
                    stateAllProcess.add(process)
                    stateAllProcess.remove(lessPriorityProcess)
                } else false
            }
        }
    }
}

enum class SortedBy {
    CREATED_DATE,
    PRIORITY,
    ID
}

class TaskManager(private val processSO: AddProcess = AddProcessDefault(MAX_CAPACITY)) {

    fun add(process: SOProcess): Boolean = processSO.add(process)
    fun kill(process: SOProcess): Boolean  = lockWrite { processSO.stateAllProcess.remove(process) }
    fun killAll(): Unit = lockWrite {
        processSO.stateAllProcess.clear()
    }
    fun killGroup(priority: Priority): Boolean = lockWrite {
        processSO.stateAllProcess.removeIf { it.priority == priority }
    }
    fun list(sortedBy: SortedBy = SortedBy.CREATED_DATE):List<SOProcess> = lockRead {
        val resultList = processSO.stateAllProcess.toMutableList()
        if(resultList.isNotEmpty()){
            resultList.sortWith(Comparator {
                    p1, p2 ->
                when(sortedBy){
                    SortedBy.CREATED_DATE -> p1.createdDate.compareTo(p2.createdDate)
                    SortedBy.PRIORITY -> p1.priority.compareTo(p2.priority)
                    SortedBy.ID -> p1.pid.compareTo(p2.pid)
                }
            })
        }

        resultList
    }
}

