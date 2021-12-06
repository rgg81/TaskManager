import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import manager.*
import model.Priority
import model.SOProcess
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.system.measureTimeMillis

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 600  // number of coroutines to launch
    val k = 2000 // times an action is repeated by each coroutine
    coroutineScope { // scope for coroutines
        repeat(n) {
            launch {
                repeat(k) { action() }
            }
        }
    }
}

private const val capacity = 30L

fun addSOProcess(taskManager: TaskManager):Unit {
    taskManager.add(SOProcess(Priority.LOW))
    taskManager.add(SOProcess(Priority.MEDIUM))
    taskManager.add(SOProcess(Priority.HIGH))
}

class TaskManagerMultiProcessingTest {

    private val taskManagerDefault = TaskManager(AddProcessDefault(capacity))
    private val taskManagerFifo = TaskManager(AddProcessFiFo(capacity))
    private val taskManagerPriority = TaskManager(AddProcessPriority(capacity))

    @Test
    fun `test multiprocessing add`(): Unit = runBlocking {
        table(
            headers("taskManager", "length"),
            row(taskManagerDefault, capacity),
            row(taskManagerFifo, capacity),
            row(taskManagerPriority, capacity),

            ).forAll { taskManager, length ->
            withContext(Dispatchers.Default) {
                massiveRun {
                    addSOProcess(taskManager)
                }
            }
            taskManager.list() shouldHaveSize length.toInt()
        }
    }

    @Test
    fun `test multiprocessing kill group`(): Unit = runBlocking {
        table(
            headers("taskManager", "length"),
            row(taskManagerDefault, 20),
            row(taskManagerFifo, 20),
            row(taskManagerPriority, 20),

            ).forAll { taskManager, length ->
            for(i in 1..10) taskManager.add(SOProcess(Priority.LOW, i.toString()))
            for(i in 11..20) taskManager.add(SOProcess(Priority.MEDIUM, i.toString()))
            for(i in 21..30) taskManager.add(SOProcess(Priority.HIGH, i.toString()))
            withContext(Dispatchers.Default) {
                massiveRun {
                    taskManager.killGroup(Priority.HIGH)
                }
            }
            taskManager.list() shouldHaveSize length
        }
    }

    @Test
    fun `test multiprocessing kill process`(): Unit = runBlocking {
        table(
            headers("taskManager", "length"),
            row(taskManagerDefault, 27),
            row(taskManagerFifo, 27),
            row(taskManagerPriority, 27),

            ).forAll { taskManager, length ->
            for(i in 1..30) taskManager.add(SOProcess(Priority.HIGH,i.toString()))
            val allProcesses = taskManager.list()
            withContext(Dispatchers.Default) {
                massiveRun {
                    taskManager.kill(allProcesses[0])
                    taskManager.kill(allProcesses[5])
                    taskManager.kill(allProcesses[15])
                }
            }
            taskManager.list() shouldHaveSize length
        }
    }

    @Test
    fun `test multiprocessing list processes`(): Unit = runBlocking {
        table(
            headers("taskManager", "sortBy", "last"),
            row(taskManagerDefault, SortedBy.CREATED_DATE, "1"),
            row(taskManagerDefault, SortedBy.PRIORITY, "31"),
            row(taskManagerDefault, SortedBy.ID, "z"),
            row(taskManagerFifo, SortedBy.CREATED_DATE, "1"),
            row(taskManagerFifo, SortedBy.PRIORITY, "31"),
            row(taskManagerFifo, SortedBy.ID, "z"),
            row(taskManagerPriority, SortedBy.CREATED_DATE, "1"),
            row(taskManagerPriority, SortedBy.PRIORITY, "31"),
            row(taskManagerPriority, SortedBy.ID, "z"),

            ).forAll { taskManager, sortBy, lastId ->
            taskManager.add(SOProcess(Priority.MEDIUM, "z"))
            taskManager.add(SOProcess(Priority.HIGH, "31"))
            for(i in 2..15) taskManager.add(SOProcess(Priority.LOW, i.toString()))
            taskManager.add(SOProcess(Priority.LOW, "1"))

            withContext(Dispatchers.Default) {
                massiveRun {
                    taskManager.list(sortBy)
                }
            }
            taskManager.list(sortBy).last().pid shouldBe lastId
        }
    }

    @Test
    fun `test multiprocessing mix`(): Unit = runBlocking {
         withContext(Dispatchers.Default) {
                massiveRun {
                    assertDoesNotThrow {
                        addSOProcess(taskManagerPriority)
                        val listProcessSO = taskManagerPriority.list()
                        if(listProcessSO.isNotEmpty()) taskManagerPriority.kill(listProcessSO.last())
                        taskManagerPriority.killGroup(Priority.LOW)
                        taskManagerPriority.killAll()
                    }
                }
            }
    }


}